package eu.wdaqua.qanary.ambiverse;

import java.util.TreeMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
/**
 * basic class for wrapping functionality to a Qanary component
 * note: there is no need to change something here
 */
public class Application {

	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent() {
		return new AmbiverseNER();
	}
	
	
    public static void main(String[] args) throws Exception {
    SpringApplication.run(Application.class, args);
    }
    
    
}

class DbpediaRecorodProperty{
	private static TreeMap<String,String> map = new TreeMap<String,String>() ;

	public static void put(String property, String dbpediaLink) {

		map.put(property, dbpediaLink);
	}

	public static TreeMap<String,String> get(){
		return map;
	}

}
