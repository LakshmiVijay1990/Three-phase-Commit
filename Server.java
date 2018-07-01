
/* Author : Lakshmi Vijay
 * Student ID : 1001576841
 * * References : https://www.youtube.com/watch?v=Uo5DY546rKY&t=445s
 *              http://net-informations.com/java/net/socket.htm
 *              http://net-informations.com/java/net/multithreaded.htm
 */
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.awt.Color;

public class Server extends JFrame
{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1871074776017997261L;
	public static ArrayList<Socket> socketList = new ArrayList<Socket>();
	public static Map<Integer,String> currentUsers = new HashMap<Integer,String>(); // list of participants who is online
	public static ArrayList<String> clientMessageInList=new ArrayList<String>(); // to store msgs sent by participants
	public static JTextArea clientMsgTxtDisplay;
	public static JScrollPane scrollPane;
	/*
	 * Constructor which sets the server GUI parameters
	 */
	public Server() 
	{
		getContentPane().setLayout(null);
		clientMsgTxtDisplay = new JTextArea();
		clientMsgTxtDisplay.setForeground(Color.GRAY);
		clientMsgTxtDisplay.setTabSize(20);
		clientMsgTxtDisplay.setBounds(46, 56, 421, 600);
		getContentPane().add(clientMsgTxtDisplay);
		JLabel lblClientMessages = new JLabel("CLIENT MESSAGES");
		lblClientMessages.setBounds(46, 26, 123, 16);
		getContentPane().add(lblClientMessages);
		
		scrollPane = new JScrollPane(clientMsgTxtDisplay);
		scrollPane.setBounds(50, 50, 500, 200);
		getContentPane().add(scrollPane);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 300);
	}
	
	public static void main(String[] args) throws IOException
	{
		try
		{
			JFrame serverWindow =new Server();
			ServerSocket server = new ServerSocket(8888);
			//Counter to keep track of the participants
			int counter=0;
			System.out.println("Server Started...");
			
			while(true)
			{
				//Server accepts the connection request
				Socket serverClient = server.accept();
				counter++;
				//Connected client added to array list of sockets
				socketList.add(serverClient);
				/*
				 * Sends request to a separate thread by calling ServerThread class 
				 * and passing parameters to its Constructor. 
				 * Parameters are the Socket serverClient,Client number, and the array list of
				 * connected client sockets 
				 */
				ServerThread sct = new ServerThread(serverClient,counter,socketList);
				Thread clientThread = new Thread(sct);
				clientThread.start();
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
}

/* 
 * Class to handle multiple threads
 */
class ServerThread implements Runnable
{
	Socket serverClient;
	ArrayList<Socket> socketList;
	int clientNo;
	
	ServerThread(Socket inSocket,int counter,ArrayList<Socket> socketList)
	{
		serverClient = inSocket;
		clientNo = counter;
		this.socketList = socketList;
	}
	//Entry point for the thread
	public void run()
	{
		try
		{	Socket temp;
			String clientMessage = "";
			String serverMessage = "";
			Scanner inStream = new Scanner(serverClient.getInputStream());
			while(true)
			{
				if(inStream.hasNext()==false) 
				{
					return;
				}
				//getting message from inStream.
				clientMessage=inStream.nextLine();
				/*
				 * Coordinator connects first to server which becomes the 
				 * first client. Clients 2,3 and 4 will be the other three
				 * participants.
				 */
				if(clientNo==1)
					serverMessage= "Coordinator sends-   " + clientMessage ;
				else
					if(clientNo>1)
						serverMessage = "Participant"+ (clientNo -1)+ "sends" + clientMessage;
				//Displaying message on the GUI
				Server.clientMsgTxtDisplay.append(serverMessage+"\n");
				/*
				 * If message contains VOTE_REQUEST, it is sent to
				 * all the participants.
				 */
				if(clientMessage.contains("VOTE_REQUEST"))
				{
					for(int i=1;i<socketList.size();i++)
					{
						temp = (Socket)socketList.get(i);
						PrintWriter outStream = new PrintWriter(temp.getOutputStream());
						outStream.println(clientMessage);
						outStream.flush();
						//inStream.close();
						
					}
				}
				/*
				 * If message contains VOTE_ABORT or VOTE_COMMIT, it is sent to
				 * Coordinator.
				 */
				else
				if(clientMessage.contains("VOTE_ABORT") || clientMessage.contains("VOTE_PRECOMMIT") || clientMessage.contains("PARTICIPANT_ACK"))
				{
					temp=socketList.get(0);
					PrintWriter outStream = new PrintWriter(temp.getOutputStream());
					outStream.println(clientMessage);
					outStream.flush();
				}
				/*
				 * If message contains GLOBAL_ABORT or GLOBAL_COMMIT, it is sent to
				 * all the participants.
				 */
				else
					if(clientMessage.contains("GLOBAL_ABORT") || clientMessage.contains("GLOBAL_COMMIT") || clientMessage.contains("ACK?"))
					{
						for(int i=1;i<socketList.size();i++)
						{
							temp=(Socket)socketList.get(i);
							PrintWriter outStream = new PrintWriter(temp.getOutputStream());
							outStream.println(i+clientMessage);
							outStream.flush();
						}
					}
				/*
				 * If message contains DESICION_REQUEST or PARTICIPANT_GLOBAL_COMMIT or PARTICIPANT_GLOBAL_ABORT,
				 *  it is sent to all the participants. 
				 */
					else
						if(clientMessage.contains("DESICION_REQUEST") || clientMessage.contains("PARTICIPANT_GLOBAL_COMMIT")|| clientMessage.contains("PARTICIPANT_GLOBAL_ABORT"))
						{
							for(int i=1;i<socketList.size();i++)
							{
								temp=(Socket)socketList.get(i);
								PrintWriter outStream = new PrintWriter(temp.getOutputStream());
								outStream.println(i+clientMessage);
								outStream.flush();
							}
						}
			}
			
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
}
