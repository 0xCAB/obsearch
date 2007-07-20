package org.ajmm.obsearch.index;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;

import org.ajmm.obsearch.Index;
import org.ajmm.obsearch.OB;
import org.ajmm.obsearch.SynchronizableIndex;
import org.ajmm.obsearch.exception.AlreadyFrozenException;
import org.ajmm.obsearch.exception.IllegalIdException;
import org.ajmm.obsearch.exception.NotFrozenException;
import org.ajmm.obsearch.exception.OBException;
import org.ajmm.obsearch.exception.OutOfRangeException;
import org.ajmm.obsearch.exception.UndefinedPivotsException;
import org.apache.log4j.Logger;

import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.DatabaseException;

/*
 OBSearch: a distributed similarity search engine
 This project is to similarity search what 'bit-torrent' is to downloads.
 Copyright (C)  2007 Arnoldo Jose Muller Molina

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * AbstractP2PIndex holds common functionality of indexes that span several
 * computers. The current implementation uses the JXTA library as network
 * infraestructure.
 * 
 * @author Arnoldo Jose Muller Molina
 * @version %I%, %G%
 * @since 0.0
 */

public abstract class AbstractP2PIndex<O extends OB> implements Index<O>, DiscoveryListener,
		PipeMsgListener {
	
	public static enum MessageType { 
		 TIME, // time
         GBOX, // global boxes
         INDEX, // local index data
         DSYNQ, // data sync query
         DSYNR, // data sync reply
         SQ, // search query
         SR // search response
	};

	private static transient final Logger logger = Logger
			.getLogger(AbstractP2PIndex.class);

	// the string that holds the original index xml
	protected String indexXml;
	
	
	// min number of pivots to have at any time... for controlling purposes
	// the necessary minimum number of peers to allow matching might be bigger than this.
	protected final int minNumberOfPeers = 5;

	// JXTA variables
	private transient NetworkManager manager;

	private transient DiscoveryService discovery;

	private final static String clientName = "OBSearchClient";

	private final static String pipeName = "OBSearchPipe";
	
	private final static int maxAdvertisementsToFind = 5;

	private final static NetworkManager.ConfigMode ConfigMode = NetworkManager.ConfigMode.ADHOC;

	private final static int maxNumberOfPeers = 100;
	
	// Interval for each heartbeat (in miliseconds)
	// heartbeats check for missing resources and make sure we are all well connected all the time
	private final static int heartBeatInterval = 30000;

	// general timeout used for most p2p operations
	private final static int globalTimeout = 60000;
	
	// if on initialization, we should wait for a rendevouz connection
	private final static boolean waitForRendevouzConnection = true;
	
	// maximum time difference between the peers.
	// peers that have bigger time differences will be dropped.
	private final static int maxTimeDifference = 3600000;

	private JxtaServerPipe serverPipe;

	// contains all the pipes that have tried to connect to us or that we have tried to connect to
	private ConcurrentMap<URI, JxtaBiDiPipe> pipes;		
	
	// time when the index was created
	protected long indexTime;
	// the index we are wrapping
	private SynchronizableIndex<O> index;
	
	// holds the boxes that this index is currently supporting
	private BitSet availableBoxes;
	
	// contains all the pipes separated by box type
	// only if our index is in full mode
	private Queue[] searchPipes;
	
	// This is used to decide which boxes we will service
	// total number of serviced boxes
	// we update them direcly if we decide to change some boxes.
	// From time to time, we send the update to everybody who is connected to us.
	// we also receive this from everybody and we update accordigly
	private int [] globalBoxCount;
	private long [] globalBoxLastUpdated;
	
	
	
	protected AbstractP2PIndex(SynchronizableIndex<O> index) throws IOException,
			PeerGroupException {
		this.index = index;
		pipes =  new ConcurrentHashMap<URI, JxtaBiDiPipe>();
		searchPipes = new Queue[index.totalBoxes()];
		availableBoxes = new BitSet(index.totalBoxes());
	}
	

	/**
	 * Initializes the p2p network
	 * @throws IOException
	 * @throws PeerGroupException
	 */
	private void init() throws IOException, PeerGroupException {
		manager = new NetworkManager(ConfigMode, clientName, new File(new File(
				".cache"), clientName).toURI());
		manager.startNetwork();
		// Get the NetPeerGroup
		PeerGroup netPeerGroup = manager.getNetPeerGroup();

		// get the discovery service
		discovery = netPeerGroup.getDiscoveryService();
		discovery.addDiscoveryListener(this);

		// init the incoming connection listener
		serverPipe = new JxtaServerPipe(manager.getNetPeerGroup(),
				getPipeAdvertisement());

		// wait for rendevouz connection
		if(this.waitForRendevouzConnection){
			logger.info("Waiting for rendevouz connection");
			manager.waitForRendezvousConnection(0);
		}
	}
	
	/**
	 * This class performs a heartbeat. It makes sure that all the resources we
	 * need to properly work.
	 * 
	 *
	 */
	private class HeartBeat implements Runnable{
		
		private boolean error = false;
		/**
		 * This method starts network connections and
		 * calls heartbeat undefinitely until the program stops 
		 */
		public void run(){
			long count = 0;
			while(! error){
				
				try{
					
					heartBeat1();
					heartBeat3(count);
					heartBeat100(count);
					
					this.wait(heartBeatInterval);
				}catch(InterruptedException i){
					if(logger.isDebugEnabled()){
						logger.debug("HeartBeat interrupted");
					}
				}catch(Exception e){
					error = true;
					logger.fatal("Exception in heartBeat", e);
					assert false;
				}
				count++;
			}
			if(error){
				logger.fatal("Stopping heartbeat because of error");
				assert false;
			}
		}
		
		// executed once per heart beat
		public void heartBeat1() throws PeerGroupException, IOException {
			    // advertisements should be proactively searched for if we are running out
			    // of connections
				findPipes();							
		}
		
		public void heartBeat3(long count) throws PeerGroupException, IOException{
			if(count % 3 == 0){
				
			}
		}
		// executed once every 100 heartbeats
		public void heartBeat100(long count) throws PeerGroupException, IOException{
			if(count % 100 == 0) {
				// send my time to everybody, telling them what is my current time
				// if someone doesn't like my time, they will desconnect from me.
				timeBeat();				
			}
		}
	}
	
	/**
	 * For each pipe in pipes, send a time message
	 *
	 */
	private void timeBeat() throws IOException{
		Iterator<URI> it = this.pipes.keySet().iterator();
		while(it.hasNext() ){			
			URI u = it.next();
			sendMessage(u, makeTimeMessage());
		}
	}
	
	private final Message makeTimeMessage()throws IOException{
		return makeTimeMessageAux(System.currentTimeMillis());
	}
	private final Message makeTimeMessageAux(long time) throws IOException{
		TupleOutput out = new TupleOutput();
		out.writeLong(time);
		Message msg = new Message();
		addMessageElement(msg, MessageType.TIME, out.getBufferBytes());		
		return msg;
	}
	
	private final long parseTimeMessage(Message msg){
		ByteArrayMessageElement m = getMessageElement(msg, MessageType.TIME);
		TupleInput in = new TupleInput(m.getBytes());
		return in.readLong();
	}
	
	/**
	 * Extracts the message associated to the given namespace
	 * Assumes that the message only contains one element
	 * @param msg
	 * @param namespace
	 * @return The ByteArrayMessageElement associated to the only MessageElement in this Message
	 */
	protected final ByteArrayMessageElement getMessageElement(Message msg, MessageType namespace){
		assert msg.getMessageNumber() == 1;
		return (ByteArrayMessageElement) msg.getMessageElement(namespace.toString(), "");
	}
	
	/**
	 * A convenience method to add a byte array to a message with the given namespace
	 * The element is added with the empty "" tag
	 * @param msg
	 * @param namespace
	 * @param b
	 * @throws IOException
	 */
	private final void addMessageElement(Message msg, MessageType namespace, byte[] b) throws IOException{		
		msg.addMessageElement(namespace.toString(), new ByteArrayMessageElement ("", MimeMediaType.AOS, b, null));
		assert msg.getMessageNumber() == 1;
	}
	
	/**
	 * Send the given message to the pipeID
	 * @param pipeID
	 * @param msg
	 * @throws IOException
	 */
	protected final void sendMessage(URI pipeID, Message msg) throws IOException{
		JxtaBiDiPipe p = pipes.get(pipeID);
		if(p != null){
			p.sendMessage(msg);
		}
	}
	
	
	/**
	 * For every pipe we have registered,
	 * we send them the globalBoxCount so that everybody has an overall idea
	 * on how many boxes are being served currently.
	 * We only need to call this method when:
	 * 1) We have changed the set of boxes we are serving
	 * 2) someone is telling us that they have done the same, and their information is more
	 *     recent than ours.
	 */
	protected void syncGlobalBoxesInformation() {
		
		
		
	}
	
	
	
	protected boolean minimumNumberOfPeers() {
		return this.pipes.size() >= this.minNumberOfPeers;
	}
		
	/**
	 * This method must be called by all users once 
	 * It starts the network, and creates some background threads like the 
	 * hearbeat and the incoming connection handler
	 */
	public void open() throws IOException, PeerGroupException {
		init();
		Thread thread = new Thread(new HeartBeat(), "Heart Beat Thread");
		thread.start();
		Thread thread2 = new Thread(new IncomingConnectionHandler(), "Incoming connection Thread");
		thread2.start();
	}
	

	private PipeAdvertisement getPipeAdvertisement() {
		PipeID pipeID = (PipeID) ID.create(generatePipeID());
		if(logger.isDebugEnabled()){
			logger.debug("Generated pipeid: " + pipeID);
		}
		PipeAdvertisement advertisement = (PipeAdvertisement) AdvertisementFactory
				.newAdvertisement(PipeAdvertisement.getAdvertisementType());

		advertisement.setPipeID(pipeID);
		advertisement.setType(PipeService.UnicastType);
		advertisement.setName(pipeName);
		return advertisement;
	}

	private URI generatePipeID() {
		return IDFactory.newPipeID(manager.getNetPeerGroup().getPeerGroupID()).toURI(); 		
	}

	private boolean readyToAcceptConnections() {
		assert false;
		return false;
	}

	// query the discovery service for OBSearch pipes
	protected void findPipes() throws IOException, PeerGroupException {
		if(! minimumNumberOfPeers()){
			discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name",
				pipeName, maxAdvertisementsToFind, null);
		}
	}

	/**
	 * Method called by the discovery server, when we discover something
	 */
	public void discoveryEvent(DiscoveryEvent ev) {

		DiscoveryResponseMsg res = ev.getResponse();
		Advertisement adv;
		Enumeration en = res.getAdvertisements();

		if (en != null) {
			while (en.hasMoreElements()) {
				adv = (Advertisement) en.nextElement();
				if (adv instanceof PipeAdvertisement) {
					PipeAdvertisement p = (PipeAdvertisement) adv;
					addPipe(p);					
				}
			}
		}
	}

	/**
	 * Adds the given pipe to our cache of pipes. The pipe is added if our quote
	 * of pipes is under the minimum.
	 * TODO: Remove synchronized ?
	 * @param p
	 */
	private synchronized  void addPipe(PipeAdvertisement p) {

		try {
			if (pipes.size() <= maxNumberOfPeers) {
				// only if we don't have already the connection
				if(! this.pipes.containsKey(p.getPipeID().toURI())){
					JxtaBiDiPipe pipe = new JxtaBiDiPipe();
					pipe.connect(manager.getNetPeerGroup(), null, p, globalTimeout,
						this);
					// 
					addPipeAux(pipe);
				}
			}
		} catch (IOException e) {
			logger.fatal("Error while trying to add Pipe:" + p + " \n " + e);
			assert false;
		}

	}
	
	/**
	 * Adds the given pipe. This is called either when an accept is made by the server,
	 * and when the discovery returns from the server.
	 * @param bidipipe
	 * @throws IOException
	 */
	private synchronized void addPipeAux(JxtaBiDiPipe bidipipe) throws IOException {
		URI pipeId = bidipipe.getPipeAdvertisement().getPipeID().toURI();
		if(pipes.containsKey(pipeId)){
			try{
				// a duplicated pipe, we should close the new one and leave the old connection open
				if(! pipes.get(pipeId).equals(bidipipe)){
					bidipipe.close();
				}				
			}catch(IOException e){
				logger.fatal("Error while trying to close a duplicated pipe" + e);
				assert false;
			}
		}else{
			this.pipes.put(bidipipe.getPipeAdvertisement().getPipeID().toURI() , bidipipe);
			// send initial sync data to make sure that everybody is
			// syncrhonized enough to be meaningful.
			sendMessagesAfterFirstEncounter(bidipipe);
		}
	}
	
	/**
	 * Obtains the PipeAdvertisement of the given pipe and
	 * if we can hold more peers, we add it to our list of peers
	 * TODO: Remove synchronized ?
	 * @param bidipipe
	 */
	private synchronized void addPipe(JxtaBiDiPipe bidipipe) throws IOException{
		if (pipes.size() <= maxNumberOfPeers) {
			addPipeAux(bidipipe);
		}else{
			try{
				bidipipe.close();
			}catch(IOException e){				
				logger.fatal("Error while closing pipe" + e);
				assert false;
			}
		}
	}

	/**
	 * This method is called when a message comes into our pipe.
	 */
	public void pipeMsgEvent(PipeMsgEvent event) {
		
		Message msg = event.getMessage();
		URI pipeId = event.getPipeID().toURI();
		
		// ************************************************
		// We handle here all the different messages we will receive.
		// ************************************************
		
		try{
			Iterator<String> it = msg.getMessageNamespaces();
			while(it.hasNext()){
				String namespace = it.next();
				MessageType messageType = MessageType.valueOf(namespace);
				
				switch (messageType) {
					case TIME: processTime(msg, pipeId); break;
				    default: assert false;
				}
				
			}
		}catch(IOException io){
			logger.fatal("Exception while receiving message", io);
			assert false;
		}
	}
	
	/**
	 * Recevies a message that contains a time MessageElement
	 * @param msg
	 */
	private void processTime(Message msg, URI pipeId) throws IOException{		
		long time = parseTimeMessage(msg);
		long ourtime = System.currentTimeMillis();
		if(logger.isDebugEnabled()){
			logger.debug("Received time " + time + " from pipe: " + pipeId);
		}
		// time must be within +- k units away from our current time.
		// otherwise we drop the connection with the given pipe id
		if(Math.abs(time - ourtime) > maxTimeDifference){
			closePipe(pipeId);
		}
	}

	/**
	 * Closes the given pipe.
	 * all resources are released.
	 */
	private void closePipe(URI pipeId) throws IOException{
		JxtaBiDiPipe pipe = pipes.get(pipeId);
		pipe.close();
	}
	
	private class IncomingConnectionHandler implements Runnable {

		public IncomingConnectionHandler() {

		}

		public void run() {

			System.out
					.println("Waiting for JxtaBidiPipe connections on JxtaServerPipe");
			while (true) {
				try {
					JxtaBiDiPipe bidipipe = serverPipe.accept();
					// add the pipe to our list of pipes, if we can hold more people.
					addPipe(bidipipe);
					
					 
				} catch (IOException e) {
					assert false;
					logger.fatal("Error while listening to a connection:" + e);
				}
			}
		}

	}
	
	/**
	 * When the peers encounter each other for the first time, we send a set of standard sync 
	 * messages only sent to the given bidipipe. This is to make sure that from the beginning we
	 * have performed all the standard sync steps. The same syncs will be performed 
	 * at various frequencies by the heart.
	 * @param bidipipe The pipe that to which we will send messages.
	 */
	private void sendMessagesAfterFirstEncounter(JxtaBiDiPipe bidipipe) throws IOException{
		if(logger.isDebugEnabled()){
			logger.debug("Sending messages after first encounter to " 
					+ bidipipe.getPipeAdvertisement().getPipeID().toURI());
		}
		// time sync message
		sendMessage(bidipipe.getPipeAdvertisement().getPipeID().toURI(), makeTimeMessage());
	}

	public void close() throws DatabaseException {
		manager.stopNetwork();
		index.close();
	}

	public int databaseSize() throws DatabaseException {
		return index.databaseSize();
	}

	public int delete(O object) throws NotFrozenException, DatabaseException {
		return index.delete(object);
	}

	public void freeze() throws IOException, AlreadyFrozenException,
			IllegalIdException, IllegalAccessException, InstantiationException,
			DatabaseException, OutOfRangeException, OBException,
			UndefinedPivotsException {
		index.freeze();

	}

	public int getBox(O object) throws OBException {
		return index.getBox(object);
	}

	public O getObject(int i) throws DatabaseException, IllegalIdException,
			IllegalAccessException, InstantiationException {
		return index.getObject(i);
	}

	public int insert(O object) throws IllegalIdException, DatabaseException,
			OBException, IllegalAccessException, InstantiationException {
		return index.insert(object);
	}

	public boolean isFrozen() {
		return index.isFrozen();
	}

	public int totalBoxes() {
		return index.totalBoxes();
	}

}