package fr.insee.duvel.troismonts;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import fr.insee.duvel.troismonts.utils.DataCubeOntology;

/**
 * The <code>DataSetModelMaker</code> class creates the Data Cube Data Set for the POP5 data set.
 * 
 * @author Franck
 */
public class DataSetModelMaker {

	private static Logger logger = LogManager.getLogger(DataSetModelMaker.class);

	private static CSVReader reader =  null;

	public static void main(String[] args) throws Exception {

		CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
		reader = new CSVReaderBuilder(new FileReader(Configuration.KARMELIETE_FILE_NAME))
				.withCSVParser(parser)
				.build();


		Model karmModel = null;
		logger.info("Creating Jena model for KARMELIET data set");
		karmModel = getDataSetModel(true);
		RDFDataMgr.write(new FileOutputStream("src/main/resources/data/ds-karm-housing.ttl"), karmModel, Lang.TURTLE);
		karmModel.close();

	}

	/**
	 * Reads the spreadsheet, extracts the data and converts it to RDF Data Cube.
	 *
	 * @param createDS Indicates if the DataSet resource should be included in the model.
	 * @return A Jena model containing the data as a Data Cube Data Set.
	 */
	public static Model getDataSetModel(boolean createDS) throws IOException, CsvValidationException {

		Model karmModel = ModelFactory.createDefaultModel();
		karmModel.setNsPrefix("rdf", RDF.getURI());
		karmModel.setNsPrefix("rdfs", RDFS.getURI());
		karmModel.setNsPrefix("xsd", XSD.getURI());
		karmModel.setNsPrefix("skos", SKOS.getURI());
		karmModel.setNsPrefix("wgs", Configuration.WGS84_NAMESPACE_URI);
		karmModel.setNsPrefix("duvel", Configuration.DUVEL_HOUSING_NAMESPACE_URI);

		Property wgsLong = karmModel.createProperty("wgs:long");
		Property wgsLat = karmModel.createProperty("wgs:lat");
		Property priceProperty = karmModel.createProperty("rdf:price");
		Property nbReviewsProperty = karmModel.createProperty("rdf:number_of_reviews");
		Property locatedInProperty = karmModel.createProperty("rdfs:locatedIn");

		// Creation of the data set
		Resource karmDataSet = karmModel.createResource(Configuration.DATASET_DUVEL_HOUSING_NAMESPACE_URI, DataCubeOntology.DataSet);
		if (createDS) {
			String label = "Housing - Logements issues de la base Inside Airbnb";
			karmDataSet.addProperty(RDFS.label, karmModel.createLiteral(label, "fr"));
			label = "Housing - Housing extracted from Inside Airbnb db";
			karmDataSet.addProperty(RDFS.label, karmModel.createLiteral(label, "en"));
			logger.info("Creating Data Set " + karmDataSet.getURI());
		}


		String[] lineInArray = reader.readNext();
		//id;name;latitude;longitude;price;number_of_reviews;city
		logger.info("Headers : "+ Arrays.toString(lineInArray) );

		while ((lineInArray = reader.readNext()) != null) {
			logger.info("Data : "+Arrays.toString(lineInArray));
			if(lineInArray.length>1) {
				// id
				String id = lineInArray[0];
				Resource housingResources = karmModel.createResource("duvel:" + id);

				// RDF type - duvel:A0001 ; rdf:type ; duvel:Housing
				housingResources.addProperty(RDF.type, "duvel:Housing");

				// name - duvel:A0001 ; skos:prefLabel ; "Chez wam"
				String name = lineInArray[1];
				housingResources.addProperty(SKOS.prefLabel, name);

				// latitude
				String lat = lineInArray[2];
				Float latFloat = Float.parseFloat(lat);
				housingResources.addProperty(wgsLat, karmModel.createTypedLiteral(latFloat));

				// longitude
				String longi = lineInArray[3];
				Float longFloat = Float.parseFloat(longi);
				housingResources.addProperty(wgsLong, karmModel.createTypedLiteral(longFloat));

				// price
				String price = lineInArray[4];
				int priceInt = Integer.parseInt(price);
				housingResources.addProperty(priceProperty, karmModel.createTypedLiteral(priceInt));

				// number_of_reviews
				String nbReviews = lineInArray[5];
				housingResources.addProperty(nbReviewsProperty, nbReviews);

				// city
				String codeGeo = lineInArray[6]; //TODO get the id on id.insee.fr sparql endpoint
				if (codeGeo.equals("75056")) {
					// Paris  : http://id.insee.fr/geo/commune/6c57acff-e2a9-4304-afc4-10b34d273374
					String idParis = "http://id.insee.fr/geo/commune/6c57acff-e2a9-4304-afc4-10b34d273374";
					housingResources.addProperty(locatedInProperty, idParis);
				}
			}
		}

		logger.info("Model complete, number of statements: " + karmModel.size());

		return karmModel;
	}
}
