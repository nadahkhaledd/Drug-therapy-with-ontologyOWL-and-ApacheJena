package drugTherapy;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;


public class Main {

	public static void main(String[] args) {
		Model model = createModelFromFile("assiWeb.owl");
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter patient name: ");
		String patientName = scanner.next();
		
		ArrayList<String>  list = ListDiseaseOfPatient(patientName, model);
		if(list.size() != 0) {
			System.out.println("\npatient diseases:");
			for (String s : list) {
				System.out.println(" " + s);
			}
		}
		
		list = ListDrugsOfPatient(patientName, model);
		if(list.size() != 0) {
			System.out.println("\npatient drugs:");
			for (String s : list) {
				System.out.println(" " + s);
			}
		}
		
		System.out.println("\nAlert:\n");
		Alert(model);
		
		scanner.close();
	}
	
	public static Model createModelFromFile(String filePath) {
		// create an empty model 
		Model model = ModelFactory.createDefaultModel(); 
		// use the FileManager to find the input file 
		InputStream in = FileManager.get().open(filePath); 
		if (in == null) { 
			throw new IllegalArgumentException( "File: not found"); 
		} 
		// read the RDF/XML file
		model.read(in, null); 
		// write it to standard out 
		//model.write(System.out);
		return model;
	}
	
	public static ArrayList<String> ListDrugsOfPatient(String PatientName, Model model) {
		ArrayList<String> arr = new ArrayList<>();
		
		String resourceURL="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#" + PatientName; 
		String TakeMedicationURI="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#TakeMedication"; 
		String MedicationNameURI = "http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#MedicationName";
		
		Property TakeMedicationProperty = model.createProperty(TakeMedicationURI); 
		Property MedicationNameProperty = model.createProperty(MedicationNameURI); 
		
		Resource offer1 = model.getResource(resourceURL); 
		
		StmtIterator iterDrugs = offer1.listProperties(TakeMedicationProperty); 
		
		while(iterDrugs.hasNext()) { 
			arr.add(iterDrugs.nextStatement() .getProperty(MedicationNameProperty).getString()); 
			}
		
		return arr;
	}
	
	public static ArrayList<String> ListDiseaseOfPatient(String PatientName, Model model) {
		ArrayList<String> arr = new ArrayList<>();
		
		String resourceURL="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#" + PatientName; 
		String HasDiseaseURI="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#HasDisease";
		String DiseaseNameURI = "http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#DiseaseName";
		
		Property HasDiseaseProperty = model.createProperty(HasDiseaseURI); 
		Property DiseaseNameProperty = model.createProperty(DiseaseNameURI); 
		
		Resource offer1 = model.getResource(resourceURL); 
		
		StmtIterator iterDiseases = offer1.listProperties(HasDiseaseProperty);
		
		while(iterDiseases.hasNext()) { 
			arr.add(iterDiseases.nextStatement() .getProperty(DiseaseNameProperty).getString()); 
			}
		
		return arr;
	}
	
	public static void Alert(Model model) {
		String PatientNameURI="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#PatientName"; 
		Property property=model.createProperty(PatientNameURI); 
		ResIterator iter = model.listSubjectsWithProperty(property); 
		while (iter.hasNext()) { 
			String name = iter.nextResource().getProperty(property) .getString();
			ArrayList<String> drugs = ListDrugsOfPatient(name, model);
			ArrayList<pair> dates = new ArrayList<>();
			for (String drug : drugs) {
				dates.add(listDatesOfDrug(drug, model));
			}
			
			
			String output = test(model,dates,drugs);
			if(output.length() != 0) {
				System.out.println(name + ":\n" + output);
			}
		}
	}
	
	public static String test(Model model, ArrayList<pair> dates, ArrayList<String> drugs) {
		String output = "";
		for(int i = 0; i < dates.size(); i++) {
			for(int j = i+1; j < dates.size(); j++) {
				if(testOverlapping(dates.get(i).start, dates.get(j).start, dates.get(i).end, dates.get(j).end)) {
					output += testInteraction(model,drugs.get(i),drugs.get(j));
				}
			}
		}
		return output;
	}
	
	public static Boolean testOverlapping(String startA, String startB, String endA, String endB) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");  
		Date dateSA;
		try {
			dateSA = sdf.parse(startA);
			Date dateEA = sdf.parse(endA); 
			Date dateSB = sdf.parse(startB);  
			Date dateEB = sdf.parse(endB); 
			return dateSA.compareTo(dateEB) <= 0 && dateEA.compareTo(dateSB) >= 0;
		} catch (ParseException e) {
			e.printStackTrace();
		}  
		return false;
	}
	
	public static String testInteraction(Model model, String drugA, String drugB) {
		String output = "";
		String resourceURLA="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#" + drugA; 
		String resourceURLB="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#" + drugB; 
		String drugNameURI = "http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#MedicationName";
		String majorURI="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#Major";
		String moderateURI = "http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#Moderate";
		String minorURI = "http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#Minor";
		
		Property drugNameProperty = model.createProperty(drugNameURI);
		Property majorProperty = model.createProperty(majorURI); 
		Property moderateProperty = model.createProperty(moderateURI); 
		Property minorProperty = model.createProperty(minorURI); 
		
		Resource offer1 = model.getResource(resourceURLA); 
		
		StmtIterator iterMajor = offer1.listProperties(majorProperty);
		StmtIterator iterModerate = offer1.listProperties(moderateProperty);
		StmtIterator iterMinor = offer1.listProperties(minorProperty);
		
		while(iterMajor.hasNext()) { 
			String name = iterMajor.nextStatement() .getProperty(drugNameProperty).getString();
			if(name == drugB) {
				output += " " + drugA + " " + drugB + " Major\n";
			}
		}
		
		while(iterModerate.hasNext()) { 
			String name = iterModerate.nextStatement() .getProperty(drugNameProperty).getString();
			if(name == drugB) {
				output += (" " + drugA + " " + drugB + " Moderate\n");
			}
		}
		
		while(iterMinor.hasNext()) { 
			String name = iterMinor.nextStatement() .getProperty(drugNameProperty).getString();
			if(name == drugB) {
				output += (" " + drugA + " " + drugB + " Minor\n");
			}
		}
		
		offer1 = model.getResource(resourceURLB);
		
		iterMajor = offer1.listProperties(majorProperty);
		iterModerate = offer1.listProperties(moderateProperty);
		iterMinor = offer1.listProperties(minorProperty);
		
		while(iterMajor.hasNext()) { 
			String name = iterMajor.nextStatement() .getProperty(drugNameProperty).getString();
			if(name == drugA) {
				output += (" " + drugB + " " + drugA + " Major\n");
			}
		}
		
		while(iterModerate.hasNext()) { 
			String name = iterModerate.nextStatement() .getProperty(drugNameProperty).getString();
			if(name == drugA) {
				output += (" " + drugB + " " + drugA + " Moderate\n");
			}
		}
		
		while(iterMinor.hasNext()) { 
			String name = iterMinor.nextStatement() .getProperty(drugNameProperty).getString();
			if(name == drugA) {
				output += (" " + drugB + " " + drugA + " Minor\n");
			}
		}
		
		return output;
	}
	
	public static pair listDatesOfDrug(String drug, Model model) {
		String resourceURL="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#" + drug; 
		String endDateURI="http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#endDate";
		String startDateURI = "http://www.semanticweb.org/nada/ontologies/2021/11/untitled-ontology-9#startDate";
		
		Property endDateProperty = model.createProperty(endDateURI); 
		Property startDateProperty = model.createProperty(startDateURI); 
		
		Resource offer1 = model.getResource(resourceURL); 
		
		StmtIterator iterstartDate = offer1.listProperties(startDateProperty);
		StmtIterator iterendDate = offer1.listProperties(endDateProperty);
		
		return new pair(iterstartDate.nextStatement().getLiteral().getString(), iterendDate.nextStatement().getLiteral().getString()); 
	}
	
	
}
