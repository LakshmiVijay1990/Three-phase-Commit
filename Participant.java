/* Author : Lakshmi Vijay
 * Student ID : 1001576841
 * * References : https://www.youtube.com/watch?v=Uo5DY546rKY&t=445s
 *              http://net-informations.com/java/net/socket.htm
 *              http://net-informations.com/java/net/multithreaded.htm
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JLabel;

public class Participant extends JFrame 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5915718280787653607L;
	static Socket socket;
	static Scanner inStream;
	static PrintWriter outStream;
	static JButton clientPrepareCommitBtn;
	static JButton clientAbortBtn;
	static String state ="INIT";
	static JTextArea clientTxtDisplay;
	static String mainMessage="";
	static JButton btnAcknowledge;
	static JLabel lblAcknowledge;
	static int counter =0,participant_counter=0,message_counter=1;
	Date currentdate = new Date();
	DateFormat dateformat= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
	static String dateString="";
	static String voteDecision="";
	static String httpStringPost="";
	public static Timer timer = new Timer();

	/*
	 * Timertask to handle coordinator crash when participant in ready
	 * or precommit state
	 */
	public static TimerTask timertask=new TimerTask() {
		
		@Override
		public void run() 
		{
			if(counter<2) 
			{
				System.out.println("sending decision request");
				voteDecision= httpStringPost+ mainMessage.length() + "\r\n"+ "Date: "+dateString 
						+ "\r\n"+"Body: "+"DESICION_REQUEST" +"\r\n";
				outStream.println(voteDecision);
				outStream.flush();
				

			}
			
		}
	};
/*
 * Timertask to handle coordinator crash while in INIT state
 */
	public static TimerTask timertask_init= new TimerTask() {
		
		@Override
		public void run() {
			if(message_counter>0 && state=="INIT")
			{
				System.out.println("Assuming Coord Crashed");
				clientAbortBtn.setEnabled(false);
				clientPrepareCommitBtn.setEnabled(false);
				btnAcknowledge.setEnabled(false);
				state = "ABORT";
				System.out.println(state);
				System.out.println("timer task init executed");
				clientTxtDisplay.setText("****ABORT****\n");
				
			}
			
		}
	};
	/*
	 * Constructor which handles GUI creation
	 */
	Participant()
	{
		httpStringPost="POST HTTP/1.1\r\n Host:localhost\r\n User-Agent: HTTPTool/1.1\r\n"
				+ "Content-Type: text/html\r\n" + "Content-Length: " ;
		dateString=  dateformat.format(currentdate) ;
		clientPrepareCommitBtn = new JButton("PREPARE COMMIT");
		clientAbortBtn = new JButton("ABORT");
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		clientAbortBtn.setEnabled(false);
		clientPrepareCommitBtn.setEnabled(false);
		clientTxtDisplay = new JTextArea();
		
		btnAcknowledge = new JButton("ACKNOWLEDGE");
		
		 lblAcknowledge = new JLabel("Please press Acknowledge");
		lblAcknowledge.setVisible(false);
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(67)
							.addComponent(clientTxtDisplay, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(29)
							.addComponent(clientPrepareCommitBtn)
							.addGap(18)
							.addComponent(clientAbortBtn)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(lblAcknowledge)
								.addComponent(btnAcknowledge))))
					.addContainerGap(66, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(32)
					.addComponent(lblAcknowledge)
					.addGap(18)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(clientAbortBtn)
						.addComponent(clientPrepareCommitBtn)
						.addComponent(btnAcknowledge))
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(clientTxtDisplay, GroupLayout.PREFERRED_SIZE, 127, GroupLayout.PREFERRED_SIZE)
					.addGap(18))
		);
		getContentPane().setLayout(groupLayout);
		setSize(500, 250);
		readFromFile();
	}//constructor close
	
	/*
	 * Function to load the previously commited string 
	 * to the display.
	 */
	public static void readFromFile()
	{
		try 
		{
			BufferedReader input = new BufferedReader(new FileReader( "Participant1.txt" ) );
			clientTxtDisplay.setText(input.readLine()+"\n");
			input.close();

		} 
		catch( IOException e ) 
		{
			System.out.println("\nProblem reading from file!\n" +"Try again.");
			e.printStackTrace();
		}
	}//closing readFromFile()
	
	/*
	 * Function to handle writing the committed string to a 
	 * non volatile storage.
	 */
	public static void writeToFile(String entailmentResult,char str) {
		try
		{
			BufferedWriter output = new BufferedWriter(
					new FileWriter( "Participant"+str+".txt" ) );

					output.write( entailmentResult);
					output.close();
		} 
		catch( IOException e ) {
			System.out.println("\nProblem writing to the output file!\n" +
					"Try again.");
			e.printStackTrace();
		}
	}
	
	/*
	 * Function to receive message from Coordinator
	 * and checks whether its a GLOBAL COMMIT or GLOBAL ABORT
	 *  or VOTE REQUEST or ACKNOWLEDGEMENT
	 * 
	 */
	public static void Receive() 
	{
		if(state=="INIT")
		{
			timer.schedule(timertask_init, 20000);
		}
		while(true)
		{	Scanner input;
		try 
		{
			input = new Scanner(socket.getInputStream());
			/*Scheduling a timer in INIT state to handle
			  Coordinator crash.
			*/
			
			if(input.hasNext())
			{
				String Message = input.nextLine();
				message_counter=0;
				if(Message.contains("VOTE_REQUEST"))
				{
					state="READY";
					clientTxtDisplay.setText("");
					clientTxtDisplay.append("State = "+state + "\n");
					clientPrepareCommitBtn.setEnabled(true);
					clientAbortBtn.setEnabled(true);
					btnAcknowledge.setEnabled(false);
					clientTxtDisplay.append(Message+"\n");
					mainMessage=Message.substring(18, Message.length());
					/*
					 * When prepare commit button is pressed, sends a vote precommit
					 * message to the coordinator
					 */
					clientPrepareCommitBtn.addActionListener(new ActionListener() 
					{
						@Override
						public void actionPerformed(ActionEvent arg0) 
						{
							
							voteDecision= httpStringPost+ mainMessage.length() + "\r\n"+ "Date: "+dateString 
									+ "\r\n"+"Body: "+"VOTE_PRECOMMIT" +"\r\n";
							outStream.println(voteDecision);
							outStream.flush();
							state="PRECOMMIT";
							clientTxtDisplay.append("State = "+state + "\n");
							//Schedules a timer for 20 seconds to wait for a global commit or abort
							timer.schedule(timertask, 20000);
						}
					});
					
					/*
					 * When ABORT button is pressed, sends a vote abort
					 * message to the coordinator
					 */
					clientAbortBtn.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent arg0) {
							voteDecision=httpStringPost+ mainMessage.length() + "\r\n"+ "Date: "+dateString 
									+ "\r\n"+"Body: "+"VOTE_ABORT" +"\r\n";
							outStream.println(voteDecision);
							outStream.flush();
							state="ABORT";
							clientTxtDisplay.append("State = "+state + "\n");
							
						}
					});
					
				}
				/*
				 * If the coordinator sends a Acknowledgement message,
				 * it will reply by pressing Acknowledgement button on GUI.
				 * Thus, it will send message participant ack .
				 */
				else
				if(Message.contains("ACK?"))
				{
					clientAbortBtn.setEnabled(false);
					clientPrepareCommitBtn.setEnabled(false);
					btnAcknowledge.setEnabled(true);
					lblAcknowledge.setVisible(true);
					btnAcknowledge.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							voteDecision=httpStringPost+ mainMessage.length() + "\r\n"+ "Date: "+dateString 
									+ "\r\n"+"Body: "+"PARTICIPANT_ACK" +"\r\n";
							outStream.println(voteDecision);
							outStream.flush();
							state="PRECOMMIT";
							clientTxtDisplay.append("State = "+state + "\n");
							
						}
					});
					
					
				}
				/*
				 * TO handle the commit in case of Coordinator crash 
				 */
				else if(Message.contains("PARTICIPANT_GLOBAL_COMMIT"))
				{
					participant_counter++;
				}
				/*
				 * TO handle the abort in case of Coordinator crash 
				 */
				else if(Message.contains("PARTICIPANT_GLOBAL_ABORT"))
				{
					counter=2;
					clientAbortBtn.setEnabled(false);
					clientPrepareCommitBtn.setEnabled(false);
					btnAcknowledge.setEnabled(false);
					state = "ABORT";
					clientTxtDisplay.append("State = "+state + "\n");
					clientTxtDisplay.append("****ABORT****\n");
					break;
				}
				else
				if(Message.contains("GLOBAL_COMMIT"))
				{	//checks if received a global commit or global abort received from
					//any of the coordinator
					counter=2;
					clientAbortBtn.setEnabled(false);
					clientPrepareCommitBtn.setEnabled(false);
					char str = Message.charAt(0);
					state = "COMMIT";
					writeToFile(mainMessage,str);
					clientTxtDisplay.append("State = "+state);
					

				}
				else 
				if(Message.contains("GLOBAL_ABORT") || (state=="ABORT"))
				{
					counter=2;
					clientAbortBtn.setEnabled(false);
					clientPrepareCommitBtn.setEnabled(false);
					btnAcknowledge.setEnabled(false);
					state = "ABORT";
					clientTxtDisplay.append("State = "+state + "\n");
					clientTxtDisplay.append("****ABORT****\n");
					break;
				}
			
				/*
				 * To handle votes in case to coordinator crash
				 */
				if(Message.contains("DESICION_REQUEST")) 
				{
					if(state.equals("PRECOMMIT"))
					{
						voteDecision= httpStringPost+ mainMessage.length() + "\r\n"+ "Date: "+dateString 
								+ "\r\n"+"Body: "+"PARTICIPANT_GLOBAL_COMMIT" +"\r\n";
						outStream.println(voteDecision);
						outStream.flush();
						clientTxtDisplay.append("State = "+state + "\n");
					
					}
					else 
					{
						voteDecision= httpStringPost+ mainMessage.length() + "\r\n"+ "Date: "+dateString 
								+ "\r\n"+"Body: "+"PARTICIPANT_GLOBAL_ABORT" +"\r\n";
						outStream.println(voteDecision);
						outStream.flush();
					}
				}
				/*
				 * if there are 3 participant global commit messages,
				 * it will go to commit state.
				 */
				if(participant_counter==3)
				{
					counter=2;
					clientAbortBtn.setEnabled(false);
					clientPrepareCommitBtn.setEnabled(false);
					char str = Message.charAt(0);
					state = "COMMIT";
					writeToFile(mainMessage,str);
					clientTxtDisplay.append("State = "+state);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		}//closing while
	}//closing Receive()
	
	public static void main(String[] args)
	{
		try
		{
			new Participant();
			try {
				socket = new Socket("127.0.0.1",8888);
				outStream =new PrintWriter(socket.getOutputStream());
				inStream = new Scanner(socket.getInputStream());
				state="INIT";
				clientTxtDisplay.append("State = "+state + "\n");
				Receive();

				
			} catch (Exception e) {

				e.printStackTrace();
			}
		}
		catch (Exception e) {
		System.out.println(e);
		}

	}
}//closing class