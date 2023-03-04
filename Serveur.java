package diskord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import tools.Database;

public class Serveur {
	private static final int PORT = 10000; // socket port
	private ServerSocket server = null;
	Connection token;
	Statement stmt;

	public Serveur(int num, Connection token) throws SQLException {

		// Connection à la BDD

		this.token = token;
		try {
			server = new ServerSocket(num);
		} catch (IOException e) {
			System.err.println("Server: " + e);
			System.exit(1);
		}

		try {
			stmt = token.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Mettre le serveur en écoute
		System.out.println("Serveur: a l'ecoute sur le port " + server.getLocalPort());
		do {
			ecoute();
		} while (true);

	}

	public class ThreadMessage extends Thread {

		Socket socket = null;
		Statement stmt;
		String user;

		/**
		 * Recupere les infos de connexion avec la BDD et le client et lance le thread
		 * 
		 * @throws SQLException
		 */
		public ThreadMessage(Socket socket, Statement stmt) throws SQLException {
			this.socket = socket;
			this.stmt = stmt;
			this.start();
		}

		/**
		 * Envoie un objet message dans la méthode pour l'envoyer au client
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
		 * Récupère un objet message envoyé par le client
		 * 
		 * @return Message
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		public Message readMessage() throws IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			return (Message) ois.readObject();
		}

		/**
		 * Envoie l'objet Message au client
		 * 
		 * @param msg
		 * @throws IOException
		 */
		public void sendMessage(Message msg) throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(msg);
		}

		@Override
		public void run() {
			try {
				System.out.println("Serveur: connexion etablie avec le client " + socket.getInetAddress());
				Message msg = new Message();
				Message rtr = new Message();
				do {
					System.out.println("en attente d'une authentification");
					msg = readMessage();

					System.out.println(msg.id);

					if (msg.id == 99) {
						break;
					}

					// si l'id vaut 98 on lance la procédure de création de compte
					if (msg.id == 98) {
						System.out.println("créer compte");
						rtr = creer_compte(msg, user);
						send(rtr);

					}

					// on vérifie si les identifiants sont bons puis on authorise l'anthentification
					if (msg.id == 1) {
						System.out.println(msg.str1);
						System.out.println(msg.str2);
						ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM user WHERE nom = \'" + msg.str1
								+ "\' AND password = \'" + msg.str2 + "\'; ");

						while (result.next()) {
							if (result.getInt("COUNT(*)") == 1) {
								rtr.connecte = true;
								user = msg.str1;
								System.out.println("l'utilisateur " + msg.str1 + " est authentifié");
							}
						}
					}

					send(rtr);
				} while (!rtr.connecte); // On attend que le client arrive à s'authentifier

				// boucle principale qui attend un message du client
				do {

					if (msg.id == 99) {
						stmt.close();
						token.close();
						System.out.println("Fermeture de la connexion à java");
						break;
					}

					msg = readMessage();

					if (msg.str1.equalsIgnoreCase("kill")) {
						stmt.close();
						token.close();
						System.out.println("Serveur arrete");
						System.exit(0);
					}

					if (msg.id == 2) {
						System.out.println("demande ami");
						rtr = demande_ami(msg, user);
						send(rtr);
					}

					if (msg.id == 3) {
						System.out.println("afficher demande envoyee");
						rtr = afficher_demande_envoyee(msg, user);
						send(rtr);
					}

					if (msg.id == 4) {
						System.out.println("afficher demande recue");
						rtr = afficher_demande_recue(msg, user);
						send(rtr);
					}

					if (msg.id == 5) {
						System.out.println("afficher liste ami");
						rtr = afficher_ami(msg, user);
						send(rtr);
					}

					if (msg.id == 6) {
						System.out.println("accepter demande ami");
						accepter_demande(msg, user);
					}

					if (msg.id == 7) {
						System.out.println("refuser demande ami");
						refuser_demande(msg, user);
					}

					if (msg.id == 8) {
						System.out.println("supprimer ami");
						supprimer_ami(msg, user);
					}
					
					if (msg.id == 9) {
						System.out.println("envoyer message");
						envoyer_message(msg, user);
					}
					
					if (msg.id == 10) {
						System.out.println("afficher message");
						rtr = afficher_message(msg, user);
						send(rtr);
					}

				} while (msg.id != 99); // On intéragit avec le client tant qu'il n'envoie pas l'information qu'il se
										// déconnecte

				System.out.println("connection rompue avec le client " + socket.getInetAddress());
				socket.close();
			} catch (IOException e) {
				System.out.println("Erreur client");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Envoie une demande d'ami
		 * 
		 * @param msg
		 * @param user
		 * @return
		 * @throws SQLException
		 */
		public Message demande_ami(Message msg, String user) throws SQLException {
			Message rtr = new Message();
			ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM user WHERE nom = \'" + msg.str1 + "\'; ");

			result.next();
			if (result.getInt("COUNT(*)") == 1) {

				result = stmt.executeQuery("SELECT COUNT(*) FROM ami WHERE (ami1 = \'" + msg.str1 + "\' and ami2 = '"
						+ user + "' ) or (ami2 = '" + msg.str1 + "' and ami1 = '" + user + "'); ");
				result.next();
				if (result.getInt("COUNT(*)") == 0) {

					result = stmt.executeQuery(
							"SELECT COUNT(*) FROM demande WHERE ami1 = '" + user + "' AND ami2 = '" + msg.str1 + "';");
					result.next();

					if (result.getInt("COUNT(*)") != 1) {

						result = stmt.executeQuery("SELECT COUNT(*) FROM demande WHERE ami1 = '" + msg.str1
								+ "' AND ami2 = '" + user + "';");
						result.next();

						if (result.getInt("COUNT(*)") != 1) {

							result = stmt.executeQuery("INSERT INTO demande VALUES ('" + user + "', '" + msg.str1
									+ "', curdate(), null);");
							rtr.value = 5;
							return rtr;

						}

						else {
							// déjà une demande en cours de la part de l'autre
							rtr.value = 4;
							return rtr;
						}

					} else {

						// déjà une demande en cours
						rtr.value = 3;
						return rtr;
					}
				} else {
					// déjà en ami
					rtr.value = 2;
					return rtr;
				}

			} else {
				// ce user n'existe pas
				rtr.value = 1;
				return rtr;
			}
		}

		/**
		 * récuperer la liste d'amis
		 * 
		 * @param msg
		 * @param user
		 * @return
		 * @throws SQLException
		 */
		public Message afficher_ami(Message msg, String user) throws SQLException {
			Message rtr = new Message();
			ResultSet result = stmt
					.executeQuery("SELECT ami1, ami2 FROM ami WHERE ami1 = '" + user + "' OR ami2 ='" + user + "'; ");
			rtr.str1 = "";

			while (result.next()) {

				if (!result.getString("ami1").equals(user)) {
					rtr.liste.add(result.getString("ami1"));
					rtr.str1 = rtr.str1 + result.getString("ami1");
				}
				if (!result.getString("ami2").equals(user)) {
					rtr.liste.add(result.getString("ami2"));
					rtr.str1 = rtr.str1 + result.getString("ami2");
				}
			}

			return rtr;
		}
		
		/**
		 * renvoie la conversation entre deux personnes
		 * @param msg
		 * @param user
		 * @return
		 * @throws SQLException
		 */
		public Message afficher_message(Message msg, String user) throws SQLException {
			Message rtr = new Message();
			System.out.println(msg.str1 + " et " + msg.str2);
			ResultSet result = stmt
					.executeQuery("SELECT ami1, msg, date from message where (ami1 = '" + msg.str1 +"' and ami2 = '" + msg.str2+ "')  "
							+ "or (ami1 = '" + msg.str2 + "' and ami2 = '" + msg.str1 + "') ; ");
			rtr.str1 = "";

			while (result.next()) {
					rtr.str1 = rtr.str1 + result.getString("ami1") + " le " + result.getString("date")+": " +result.getString("msg") + "\n";
			}

			System.out.println("ok");
			return rtr;
		}

		/**
		 * récuperer la liste des demandes envoyées
		 * 
		 * @param msg
		 * @param user
		 * @return
		 * @throws SQLException
		 */
		public Message afficher_demande_envoyee(Message msg, String user) throws SQLException {
			Message rtr = new Message();
			ResultSet result = stmt.executeQuery("SELECT ami2 FROM demande WHERE ami1 = '" + user + "'; ");
			rtr.str1 = "";

			while (result.next()) {
				rtr.liste.add(result.getString("ami2"));
				rtr.str1 = rtr.str1 + result.getString("ami2");
			}

			return rtr;
		}

		/**
		 * récupérer la liste des demandes reçues
		 * 
		 * @param msg
		 * @param user
		 * @return
		 * @throws SQLException
		 */
		public Message afficher_demande_recue(Message msg, String user) throws SQLException {
			Message rtr = new Message();
			ResultSet result = stmt.executeQuery("SELECT ami1 FROM demande WHERE ami2 = '" + user + "'; ");
			rtr.str1 = "";

			while (result.next()) {

				rtr.liste.add(result.getString("ami1"));
				rtr.str1 = rtr.str1 + result.getString("ami1") + " ";
			}

			return rtr;
		}

		/**
		 * accepter la demande d'ami
		 * 
		 * @param msg
		 * @param user
		 * @throws SQLException
		 */
		public void accepter_demande(Message msg, String user) throws SQLException {
			stmt.executeQuery("delete from demande where ami1 = '" + msg.str1 + "' and ami2 = '" + user + "';");
			stmt.executeQuery("insert into ami values ('" + msg.str1 + "', '" + user + "', curdate(), null);");

		}

		/**
		 * retirer l'ami
		 * 
		 * @param msg
		 * @param user
		 * @throws SQLException
		 */
		public void supprimer_ami(Message msg, String user) throws SQLException {
			stmt.executeQuery("delete from ami where (ami1 = '" + user + "' and ami2 = '" + msg.str1 + "') or (ami2 = '"
					+ user + "' and ami1 = '" + msg.str1 + "')");

		}
		
		/**
		 * Envoie un message
		 * 
		 * @param msg
		 * @param user
		 * @throws SQLException
		 */
		public void envoyer_message(Message msg, String user) throws SQLException {
			
			stmt.executeQuery("insert into message values ('" + msg.str1 + "', '" + msg.str2 + "', curdate(), '" + msg.str3 + "',null);");
		}

		/**
		 * refuser la demande d'ami
		 */
		public void refuser_demande(Message msg, String user) throws SQLException {
			stmt.executeQuery("delete from demande where ami1 = '" + msg.str1 + "' and ami2 = '" + user + "';");
		}

		/**
		 * annuler la demande d'ami
		 * 
		 * @param msg
		 * @param user
		 * @throws SQLException
		 */
		public void annuler_demande(Message msg, String user) throws SQLException {
			stmt.executeQuery("delete from demande where ami1 = '" + user + "' and ami2 = '" + msg.str1 + "';");
		}

		
		/**
		 * créer un compte
		 * 
		 * @param msg
		 * @param user
		 * @return
		 * @throws SQLException
		 */
		public Message creer_compte(Message msg, String user) throws SQLException {
			Message rtr = new Message();
			ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM user WHERE nom = '" + msg.str1 + "'; ");

			while (result.next()) {

				if (result.getInt("COUNT(*)") == 0) {

					stmt.executeQuery("INSERT INTO user VALUES ('" + msg.str1 + "', '" + msg.str2 + "');");
					rtr.id = 1;

				} else {
					rtr.id = 0;
				}
			}

			return rtr;
		}

	}

	/**
	 * Ouvre un nouvea thread à chaque nouvelle connexion avec un client
	 * 
	 * @throws SQLException
	 */
	public void ecoute() throws SQLException {
		try {
			new ThreadMessage(server.accept(), this.stmt);
		} catch (IOException e) {
			System.out.println("Erreur client");
		}
	}

	/**
	 * receive from the socket a string message and send it in upper case to the
	 * same socket
	 * 
	 * @throws SQLException
	 */
	public static void main(String args[]) throws SQLException {

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("properties/configuration.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// get the setting
		String host = properties.getProperty("db_host");
		String user = properties.getProperty("db_user");
		String pwd = properties.getProperty("db_pwd");
		String dbname = properties.getProperty("db_name");

		// connect to the database (see Database.java class)
		Connection connection = Database.getConnection(host, user, pwd, dbname);

		int num;
		if (args.length == 0)
			new Serveur(PORT, connection);
		else if ((args.length == 1) && ((num = Integer.parseInt(args[0])) > 0))
			new Serveur(num, connection);
		else {
			System.out.println("Usage: java MessageServeur [<num_port>]");
			System.exit(1);
		}
	}
}
