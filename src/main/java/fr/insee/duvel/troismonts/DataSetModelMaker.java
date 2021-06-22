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


		String inputFile = Configuration.KARMELIETE_FILE_NAME;

		String outputFile = "src/main/resources/data/ds-karm-housing-paris.ttl";

		if(System.getenv("INPUT_FILE")!=null){
			inputFile = System.getenv("INPUT_FILE");;
		}
		if(System.getenv("OUTPUT_FILE")!=null){
			inputFile = System.getenv("OUTPUT_FILE");;
		}

		CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
		reader = new CSVReaderBuilder(new FileReader(inputFile))
				.withCSVParser(parser)
				.build();


		Model karmModel = null;
		logger.info("Creating Jena model for KARMELIET data set");
		karmModel = getDataSetModel(true);
		RDFDataMgr.write(new FileOutputStream(outputFile), karmModel, Lang.TURTLE);
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
		karmModel.setNsPrefix("gn", Configuration.GN_NAMESPACE_URI);
		karmModel.setNsPrefix("duvel", Configuration.DUVEL_HOUSING_NAMESPACE_URI);
		karmModel.setNsPrefix("geo", Configuration.GEO_NAMESPACE_URI);


		Property wgsLong = karmModel.createProperty("wgs:long");
		Property wgsLat = karmModel.createProperty("wgs:lat");
		Property priceProperty = karmModel.createProperty("rdf:price");
		Property nbReviewsProperty = karmModel.createProperty("rdf:number_of_reviews");
		Property locatedInProperty = karmModel.createProperty("gn:locatedIn");
		Property geoAsWKTProperty = karmModel.createProperty("geo:asWKT");

		String[] lineInArray = reader.readNext();
		//id;name;latitude;longitude;price;number_of_reviews;city
		logger.info("Headers : "+ Arrays.toString(lineInArray) );

		while ((lineInArray = reader.readNext()) != null) {
			logger.info("Data : "+Arrays.toString(lineInArray));
			if(lineInArray.length>1) {
				// id
				String id = lineInArray[0];
				Resource housingResources = karmModel.createResource("duvel:" + id);

				Resource housingOnject = karmModel.createResource("duvel:Housing");
				// RDF type - duvel:A0001 ; rdf:type ; duvel:Housing
				housingResources.addProperty(RDF.type,housingOnject);

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
				Float priceFloat = Float.parseFloat(price);
				housingResources.addProperty(priceProperty, karmModel.createTypedLiteral(priceFloat));

				// number_of_reviews
				String nbReviews = lineInArray[5];
				housingResources.addProperty(nbReviewsProperty, nbReviews);

				// city
				String codeGeo = lineInArray[6]; //TODO get the id on id.insee.fr sparql endpoint
				if (codeGeo.equals("75056")) {
					// Paris  : http://id.insee.fr/geo/commune/6c57acff-e2a9-4304-afc4-10b34d273374
					String idParis = "http://id.insee.fr/geo/commune/6c57acff-e2a9-4304-afc4-10b34d273374";
					Resource locatingResources = karmModel.createResource(idParis);
					housingResources.addProperty(locatedInProperty, locatingResources);
				}

				// point
				//http://www.opengis.net/ont/geosparql#asWKT	"POINT(997322 6744741.1)"^^http://www.opengis.net/ont/geosparql#wktLiteral
				//String asWKT = "POINT("+lat+" "+longi+")";
				String asWKT = String.format("POINT(%s %s)", lat, longi);

				housingResources.addProperty(geoAsWKTProperty, karmModel.createTypedLiteral(asWKT,"geo:wktLiteral"));

			}
		}

		logger.info("Model complete, number of statements: " + karmModel.size());

		return karmModel;
	}
}
