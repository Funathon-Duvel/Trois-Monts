package fr.insee.duvel.troismonts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.vocabulary.*;

import fr.insee.duvel.troismonts.utils.DataCubeOntology;


public class Configuration {


	/** Name of the CSV file containing the Airbnb data
	 * The source file is on Kubernetes S3 after Karmeliete pipilne execution */
	public final static String KARMELIETE_FILE_NAME = "src/main/resources/data/karmeliete-output.csv";

	public static final String DUVEL_SPARQL_ENDPOINT = "http://duvel.dev.insee.io/sparql";
	public static final String DUVEL_HOUSING_NAMESPACE_URI = "https://duvel.dev.insee.io/housing";
	public static final String DATASET_DUVEL_HOUSING_NAMESPACE_URI = "https://duvel.dev.insee.io/housing/dataset";
	public static final String WGS84_NAMESPACE_URI = "http://www.w3.org/2003/01/geo/wgs84_pos#";

	public final static int FIRST_DATA_LINE_INDEX = 2;

}
