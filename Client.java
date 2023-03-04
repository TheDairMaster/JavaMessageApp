package diskord;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Socket socket = null;
	Message msg = new Message();
	Message rtr = new Message();
	String username;
	
	
	JTextArea zonemessage;
	DefaultListModel<String> model = new DefaultListModel<String>();
	JList<String> liste_ami;

	public Client(int num) throws ClassNotFoundException {

		/**
		 * Connexion au serveur
		 */
		try {
			socket = new Socket("127.0.0.1", num);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Client: " + e);
			System.exit(1);
		}

		JPanel panel = new JPanel();
		JPanel panelpopup = new JPanel();

		JTextField nom = new JTextField(15);
		JTextField password = new JTextField(15);

		panelpopup.add(new JLabel("nom d'utilisateur :"));
		panelpopup.add(nom);
		panelpopup.add(new JLabel("mot de passe:"));
		panelpopup.add(password);

		se_connecter(panelpopup, nom, password);

		/**
		 * Structure du client
		 * 
		 */

		JMenuBar barremenu = new JMenuBar();
		setJMenuBar(barremenu);

		JMenu menu = new JMenu("Menu");
		JMenu ami = new JMenu("Amis");
		barremenu.add(menu);
		barremenu.add(ami);

		JMenuItem quitter = new JMenuItem("Quitter");

		quitter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				msg.id = 99;
				send(msg);

				try {
					socket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.exit(0);
			}

		});

		menu.add(quitter);

		JMenuItem ajouterami = new JMenuItem("Envoyer une demande d'ami");

		ajouterami.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String demandeami = JOptionPane.showInputDialog("Entrez le nom de l'ami que vous souhaitez ajouter");

				if (demandeami == username) {
					JOptionPane.showMessageDialog(panel, "Vous ne pouvez pas vous ajouter vous-même en ami");
				} else if (demandeami != null) {
					msg.str1 = demandeami;
					msg.id = 2;
					send(msg);
					rtr = recieve();

					if (rtr.value == 1) {
						JOptionPane.showMessageDialog(panel, "Cet utilisateur n'existe pas");
					} else if (rtr.value == 3) {
						JOptionPane.showMessageDialog(panel, "Vous avez déjà envoyé une demande à " + demandeami);

					} else if (rtr.value == 2) {
						JOptionPane.showMessageDialog(panel, "Vous êtes déjà ami avec " + demandeami);
					} else if (rtr.value == 4) {
						JOptionPane.showMessageDialog(panel, demandeami + " vous a déjà envoyé une demande d'ami");
					} else if (rtr.value == 5) {
						JOptionPane.showMessageDialog(panel, "Demande d'ami envoyée à : " + demandeami);
					} else {
						JOptionPane.showMessageDialog(panel, "Erreur");
					}
				}

				refresh();
			}
		});

		ami.add(ajouterami);

		JMenuItem retirerami = new JMenuItem("Retirer un ami");
		ami.add(retirerami);

		retirerami.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				msg.id = 5;
				send(msg);
				rtr = recieve();

				if (rtr.liste.size() != 0) {
					String name;
					String[] tabdemande = new String[rtr.liste.size()];
					System.out.println(rtr.liste.size());
					for (int i = 0; i < tabdemande.length; i++) {
						tabdemande[i] = rtr.liste.get(i);
						System.out.println(tabdemande[i]);
					}
					name = alertesuppr(tabdemande);

					if (name != null) {
						msg.str1 = name;
						msg.id = 8;
						send(msg);
						JOptionPane.showMessageDialog(panel, "Vous n'êtes maintenant plus ami avec " + name);
					}
				} else {
					JOptionPane.showMessageDialog(panel, "Vous n'avez pas d'ami");
				}

				refresh();
			}
		});

		JMenuItem accepterdemande = new JMenuItem("Accepter une demande d'ami");
		ami.add(accepterdemande);

		accepterdemande.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				msg.id = 4;
				send(msg);
				rtr = recieve();

				if (rtr.liste.size() != 0) {
					String name;
					String[] tabdemande = new String[msg.liste.size() + 1];
					System.out.println(msg.liste.size());
					for (int i = 0; i < tabdemande.length; i++) {
						tabdemande[i] = rtr.liste.get(i);
					}
					name = alertedemande(tabdemande);

					if (name != null) {
						msg.str1 = name;
						msg.id = 6;
						send(msg);
						refresh();
						JOptionPane.showMessageDialog(panel, "Vous êtes maintenant ami avec " + name);
					}
				} else {
					JOptionPane.showMessageDialog(panel, "Vous n'avez reçu aucune demande");
				}

				refresh();

			}
		});

		JMenuItem refuserdemande = new JMenuItem("Refuser une demande d'ami");
		ami.add(refuserdemande);

		refuserdemande.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				msg.id = 4;
				send(msg);
				rtr = recieve();

				if (rtr.liste.size() != 0) {
					String name;
					String[] tabdemande = new String[msg.liste.size() + 1];
					System.out.println(msg.liste.size());
					for (int i = 0; i < tabdemande.length; i++) {
						tabdemande[i] = rtr.liste.get(i);
					}
					name = alertedemanderefus(tabdemande);

					if (name != null) {
						msg.str1 = name;
						msg.id = 7;
						send(msg);
						JOptionPane.showMessageDialog(panel, "Vous avez refusé la demande de " + name);
					}
				} else {
					JOptionPane.showMessageDialog(panel, "Vous n'avez reçu aucune demande");
				}

				refresh();

			}
		});

		JMenuItem annulerdemande = new JMenuItem("Annuler une demande d'ami");
		ami.add(annulerdemande);

		barremenu.add(Box.createHorizontalGlue());

		JLabel nom_connecte = new JLabel("Vous etes connecte en tant que : " + msg.str1 + "\n");
		barremenu.add(nom_connecte);

		JLabel padding = new JLabel("    ");
		barremenu.add(padding);

		this.setLayout(new BorderLayout());

		zonemessage = new JTextArea("Choisissez un ami pour discuter avec lui");
		zonemessage.setEditable(false);

		JPanel zoneenvoi = new JPanel();
		zoneenvoi.setLayout(new BorderLayout());

		JTextField ecriremessage = new JTextField();
		zoneenvoi.add(ecriremessage, BorderLayout.CENTER);

		JButton envoyermessage = new JButton("Envoyer");
		
		envoyermessage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				msg.str1 = username;
				msg.id = 9;
				msg.str3 = ecriremessage.getText();
				send(msg);

				msg.id = 10;
				send(msg);
				rtr = recieve();
				afficher_message();
				
				refresh();
			}
		});
		
		zoneenvoi.add(envoyermessage, BorderLayout.EAST);

		this.add(zoneenvoi, BorderLayout.SOUTH);
		this.add(new JScrollPane(zonemessage), BorderLayout.CENTER);
		
		
		msg.id = 5;
		send(msg);
		rtr = recieve();
		String[] tabl = rtr.liste.toArray(new String[0]);
		for (int c = 0; c < tabl.length; c++) {
			model.addElement(tabl[c]);
		}

		liste_ami = new JList<String>(model);

		liste_ami.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent me) {
				if (me.getClickCount() == 1) {
					JList<?> target = (JList<?>) me.getSource();
					int index = target.locationToIndex(me.getPoint());
					if (index >= 0) {
						Object item = target.getModel().getElementAt(index);
						msg.id = 10;
						
						msg.str1 = username;
						msg.str2 = item.toString();
						System.out.println(msg.str1 + " et " + msg.str2);
						send(msg);
						rtr = recieve();
						afficher_message();
					}
				}
			}

		}

		);

		this.add(liste_ami, BorderLayout.WEST);

		// FIN DE LA CONSTRUCTION DE L'APPLICATION

		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {

				msg.id = 99;
				send(msg);

				try {
					socket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e.getWindow().dispose();
				System.out.println("Fenetre fermee");
				System.exit(0);
			}
		});

		this.setTitle("Diskord");
		// this.setIconImage();
		this.setSize(700, 450);
		this.setLocation(600, 300);
		this.setVisible(true);

		// envoie();
	}

	
	// fonctions qui servent à afficher les popups de connexion
	public String alertedemande(String[] tabdemande) {

		return (String) JOptionPane.showInputDialog(this, "Choissisez un utilisateur", "Accepter une demande",
				JOptionPane.QUESTION_MESSAGE, null, tabdemande, "Demande");
	}

	public String alertedemanderefus(String[] tabdemande) {

		return (String) JOptionPane.showInputDialog(this, "Choissisez un utilisateur", "Refuser une demande",
				JOptionPane.QUESTION_MESSAGE, null, tabdemande, "Demande");
	}

	public String alertesuppr(String[] tabdemande) {

		return (String) JOptionPane.showInputDialog(this, "Choissisez un utilisateur", "Supprimer cet ami",
				JOptionPane.QUESTION_MESSAGE, null, tabdemande, "Demande");
	}

	public String alerteannuler(String[] tabdemande) {

		return (String) JOptionPane.showInputDialog(this, "Choissisez un utilisateur", "Annuler la demande",
				JOptionPane.QUESTION_MESSAGE, null, tabdemande, "Demande");
	}

	
	/**
	 * affiche une conversation
	 */
	public void afficher_message() {
		
		String liste_message;
		
		liste_message = "Conversation avec " + msg.str2 + "\n\n";
		
		
		zonemessage.setText(liste_message + rtr.str1);
		System.out.println(rtr.str1);
	}
	
	
	/**
	 * raffraichit la liste d'amis
	 */
	public void refresh() {

		model.clear();
		msg.id = 5;
		send(msg);
		rtr = recieve();

		String[] tabl = rtr.liste.toArray(new String[0]);

		for (int c = 0; c < tabl.length; c++) {
			model.addElement(tabl[c]);
		}

		liste_ami = new JList<String>(model);
		liste_ami.updateUI();

	}

	/**
	 * Popup de création de compte
	 * @param panelpopup
	 * @param nom
	 * @param password
	 */
	public void creercompte(JPanel panelpopup, JTextField nom, JTextField password) {

		int result;
		String info_connexion = "Choisissez un nom d'utilisateur et un mdp";
		String[] options = { "Créer un compte", "Quitter", "Annuler" };

		result = JOptionPane.showOptionDialog(null, panelpopup, info_connexion, JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, null);

		System.out.println(result);

		if (result == 1) {
			msg.id = 99;
			send(msg);

			try {
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("Fenetre fermee");
			System.exit(0);
			;
		}

		if (((!nom.getText().isBlank()) || (!nom.getText().isBlank())) || result == 0) {
			msg.str1 = nom.getText();
			msg.str2 = password.getText();
			msg.id = 98;
			send(msg);
			rtr = recieve();

			System.out.println(rtr.id);

			if (rtr.id == 1) {
				JOptionPane.showMessageDialog(this, "Le compte " + msg.str1 + " a bien été créé");
			} else {
				JOptionPane.showMessageDialog(this, "Le compte " + msg.str1 + " existe déjà");
			}

		} else {
			if (result == 2) {

			} else {
				JOptionPane.showMessageDialog(this, "Veuillez entrer des valeurs valides");
			}
		}

	}

	
	/**
	 * tente de s'authentifier auprès du serveur
	 * 
	 * @param panelpopup
	 * @param nom
	 * @param password
	 */
	public void se_connecter(JPanel panelpopup, JTextField nom, JTextField password) {

		int result;
		String info_connexion = "Entrez vos identifiants de connexion";
		String[] options = { "Se connecter", "Quitter", "Créer un compte" };

		do {
			result = JOptionPane.showOptionDialog(null, panelpopup, info_connexion, JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, null);

			System.out.println(result);

			if (result == 1) {
				msg.id = 99;
				send(msg);

				try {
					socket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.out.println("Fenetre fermee");
				System.exit(0);
				;
			}
			if (result == 2) {
				creercompte(panelpopup, nom, password);
			}

			if (((!nom.getText().isBlank()) || (!nom.getText().isBlank())) || result == 0) {
				msg.str1 = nom.getText();
				msg.str2 = password.getText();
				msg.id = 1;
				send(msg);
				rtr = recieve();
				if (!rtr.connecte)
					info_connexion = "Utilisateur/MDP incorrecte";

			} else
				info_connexion = "Utilisateur/MDP incorrecte";
		} while (!rtr.connecte);
		username = msg.str1;

	}

	/**
	 * envoie un message au serveur
	 * 
	 * @param msg
	 */
	public void send(Message msg) {
		try {
			sendMessage(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * reçoie un message du serveur
	 * 
	 * @return
	 */
	public Message recieve() {
		try {
			return readMessage();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void envoie() throws ClassNotFoundException {
		try {
			System.out.println("Client: connexion etablie avec le serveur " + socket.getInetAddress());
			Message msg = new Message();
			Scanner clavier = new Scanner(System.in);
			String message;
			do {
				System.out.println("Entrez le message : ");
				message = clavier.nextLine();
				msg.lecture(message);
				sendMessage(msg);
				System.out.println("Message envoye");
			} while ((!msg.text.equalsIgnoreCase("exit")) | (!msg.text.equalsIgnoreCase("kill")));
			clavier.close();
			System.out.println("connection rompue avec le serveur " + socket.getInetAddress());
			socket.close();
			System.exit(0);
		} catch (IOException e) {
			System.out.println("Erreur serveur");
		}
	}

	public Message readMessage() throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		Message mes = (Message) ois.readObject();
		return mes;
	}

	public void sendMessage(Message msg) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(msg);
	}

	public static void main(String args[]) {
		int num = 0;
		if (args.length == 0)
			try {
				new Client(10000);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		else if ((args.length == 1) && ((num = Integer.parseInt(args[0])) > 0))
			try {
				new Client(num);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		else {
			System.out.println("Usage: java MajClient [<num_port>]");
			System.exit(1);
		}
	}

}
