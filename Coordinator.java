/* Author : Lakshmi Vijay
 * Student ID : 1001576841
 * * References : https://www.youtube.com/watch?v=Uo5DY546rKY&t=445s
 *              http://net-informations.com/java/net/socket.htm
 *              http://net-informations.com/java/net/multithreaded.htm
 */

import javax.swing.JFrame;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;


public class Coordinator extends JFrame
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -152012826177767506L;
	static Socket socket;
	static Scanner inStream;
	//static BufferedReader inStream;
	static PrintWriter outStream;
	public static Coordinator obj;
	static String coordinatorMessage;
	static String state= "INIT";
	static int preCommitCounter=0;
	static int counter=0;
	static String httpStringGet="",dateString="",messageToClients="";
	static Date currentdate = new Date();
	static DateFormat dateformat= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
	static Timer timer = new Timer();
	static JButton btnSendToClient = new JButton("SEND TO CLIENTS");
	public static JTextField coordMsgSend;
	public	JTextField coordMsgDisplay;
	public static JScrollPane scrollPane;
	/*
	 * Constructor to set up the GUI parameters
	 */

	public Coordinator()
	{
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		coordMsgDisplay = new JTextField();
		coordMsgDisplay.setBounds(0, 0, 0, 0);
		coordMsgSend = new JTextField();
		coordMsgSend.setBounds(6, 28, 213, 26);
		getContentPane().setLayout(null);
		getContentPane().add(coordMsgDisplay);
		coordMsgDisplay.setColumns(10);
		setVisible(true);
		getContentPane().add(coordMsgSend);
		coordMsgSend.setColumns(10);
		JLabel lblEnterTextHere = new JLabel("Enter Text Here");
		lblEnterTextHere.setBounds(6, 6, 117, 16);
		btnSendToClient.setBounds(256, 28, 149, 29);
		getContentPane().add(btnSendToClient);
		getContentPane().add(lblEnterTextHere);
		
		setSize(500, 300);
		dateString=  dateformat.format(currentdate) ;

		httpStringGet="POST HTTP/1.0\r\n User-Agent: HTTPTool/1.1\r\n"
				+ "Content-Type: text/html\r\n" + "Content-Length\r\n" + dateformat.format(currentdate) 
				+"Body";
		/*
		 * When the button is pressed, it sends the message to all three participants.
		 * It enters state WAIT after sending the request.
		 */
		btnSendToClient.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				messageToClients=coordMsgSend.getText();
				coordinatorMessage = httpStringGet + messageToClients.length() +"\r\n"+ "Date: "+dateString 
						+ "\r\n"+"Body: "+"VOTE_REQUEST" + messageToClients+"\r\n";
				outStream.println(coordinatorMessage);
				outStream.flush();

				state = "WAIT";
				System.out.println("State = "+state);
				/*
				 * After sending the message, it schedules a timer for 20 seconds,
				 * so that if no reply from participants, it will abort.
				 */
				if(state=="WAIT")
				{

					timer.schedule(timertask, 20000);
					timerfunction();
				}

			}

		});



	}//closing Constructor

	/*
	 * Timer Task scheduled to take place after
	 * 20 seconds which sends a global abort
	 */
	static TimerTask timertask=new TimerTask() 
	{
		@Override
		public void run() 
		{
			if(counter!=3 && preCommitCounter!=4)
			{
				
				sendFinalResultToClient("GLOBAL_ABORT");
				state="ABORT";
				System.out.println("State = "+state);
			}
		}
	};
	




	/*
	 * Function to send the final Decision to all the clients
	 * whether its a global commit or global abort.
	 */
	public static void sendFinalResultToClient(String decision)
	{

		coordinatorMessage = httpStringGet + decision.length() +"\r\n"+ "Date: "+dateString 
				+ "\r\n"+"Body: "+decision+"\r\n";
		//sending the message to server
		outStream.println(coordinatorMessage);
		outStream.flush();

	}

	/*
	 * Function to handle the different votes coming participants
	 */
	public static void timerfunction()
	{
		String clientMessage="";
		String finaldecision= "GLOBAL_ABORT";
		
		
		while(true)
		{
			Scanner inStreamtemp;
			try
			{
				inStreamtemp = new Scanner(socket.getInputStream());

				if(inStreamtemp.hasNext())
				{
					clientMessage=inStreamtemp.nextLine();
					//clientMessage=inStream.readLine();
					

					if(clientMessage.contains("PARTICIPANT_ACK")) 
					{
						finaldecision = "GLOBAL_COMMIT";
						System.out.println("Received Acknowledgement");
						counter ++;
					}
					else
						if(clientMessage.contains("VOTE_ABORT"))
						{
							finaldecision = "GLOBAL_ABORT";
							System.out.println(finaldecision);
							state="ABORT";
							counter=3;
							System.out.println("State = "+ state);
							sendFinalResultToClient(finaldecision);
							break;
						}
						else
							if(clientMessage.contains("VOTE_PRECOMMIT"))
							{	
								preCommitCounter++;
								finaldecision="ACK?";


							}
				}
				else 
				{
					System.out.println("No Message");
				}
				/*
				 * To check whether coordinator received 
				 *  precommits from all three participants
				 */
				if( preCommitCounter==3)
				{
					sendFinalResultToClient(finaldecision);
					state="PRECOMMIT";
					preCommitCounter=4;
					System.out.println("States ="+ state);

				}
				/*
				 * To check whether coordinator received 
				 * acknowledgements from all three participants
				 */
				if(counter==3)
				{	state="COMMIT";
					sendFinalResultToClient(finaldecision);
					System.out.println("State = "+state+"\n");
					break;
				}
			}//closing while(true)

			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

	} //closing timerfunction()



	public static  void main(String[] args)
	{	

		try
		{
			socket = new Socket("127.0.0.1",8888);
			outStream =new PrintWriter(socket.getOutputStream());
			//inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			inStream= new Scanner(socket.getInputStream());
			
			state="INIT";
			
			System.out.println("State = "+state);
			obj=new Coordinator();
			

			obj.addWindowListener(new WindowListener() {

				@Override
				public void windowOpened(WindowEvent arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void windowIconified(WindowEvent arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void windowDeiconified(WindowEvent arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void windowDeactivated(WindowEvent arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void windowClosing(WindowEvent arg0) {
					try {
						socket.close();
						System.out.println( "Coordinator Crashed" );
						System.exit(0);
					} catch (IOException e) {
						
						e.printStackTrace();
					}
					
				}

				@Override
				public void windowClosed(WindowEvent arg0) {
					// TODO Auto-generated method stub

				}

				@Override
				public void windowActivated(WindowEvent arg0) {
					// TODO Auto-generated method stub

				}
			});


		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}

	}//closing main()
}//closing class Coordinator
