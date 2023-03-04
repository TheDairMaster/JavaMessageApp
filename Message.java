package diskord;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	boolean connecte = false;
	int id = 0; // Le champ id va servir à identifier le contenu de l'objet : message / demande d'ami / deconnexion etc
	int value = 0;
	String text;
	ArrayList<String> liste = new ArrayList<String>(); // liste de chaines de caracteres
	int number = 0;
	String str1, str2, str3, str4; // Les autres variables vont servir à échanger des informations

	public void lecture(String msg) {
		this.text = msg;
		this.number += 1;
	}

	@Override
	public String toString() {
		return "Message #" + Integer.toString(number) + ": " + text;
	}
}
