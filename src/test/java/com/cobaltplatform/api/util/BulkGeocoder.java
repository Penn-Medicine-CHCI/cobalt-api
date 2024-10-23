package com.cobaltplatform.api.util;

import com.cobaltplatform.api.integration.google.DefaultGoogleGeoClient;
import com.cobaltplatform.api.integration.google.GoogleGeoClient;
import com.cobaltplatform.api.integration.google.model.NormalizedPlace;
import com.google.maps.places.v1.SearchTextRequest;
import com.google.maps.places.v1.SearchTextResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class BulkGeocoder {
	public static void main(String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		String apiKey = Files.readString(Path.of("resources/test/google-maps-api-key"), StandardCharsets.UTF_8);
		String geoServiceAccountPrivateKeyJson = Files.readString(Path.of("resources/test/geo-service-account-private-key.json"), StandardCharsets.UTF_8);

		Path inputFile = Path.of("resources/test/bulk_geocode_input_addresses.csv");

		if (!Files.exists(inputFile))
			throw new IllegalStateException(format("Input file %s does not exist", inputFile.toAbsolutePath()));

		Path outputFile = Path.of("resources/test/bulk_geocode_output_addresses.csv");

		if (Files.isDirectory(outputFile))
			throw new IllegalStateException(format("Output file %s is a directory", outputFile.toAbsolutePath()));

		// 1. Read in the input file rows
		List<InputAddress> inputAddresses = new ArrayList<>();

		try (Reader reader = new FileReader(inputFile.toFile(), StandardCharsets.UTF_8)) {
			for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
				InputAddress inputAddress = new InputAddress();
				inputAddress.setId(trimToNull(record.get("id")));
				inputAddress.setName(trimToNull(record.get("name")));
				inputAddress.setAddress(trimToNull(record.get("address")));

				inputAddresses.add(inputAddress);
			}
		}

		Map<String, NormalizedPlace> normalizedPlacesByInputAddressId = new HashMap<>(inputAddresses.size());
		int processedRows = 0;
		int processedRowLimit = 5;

		// 2. Geocode the input file rows
		try (GoogleGeoClient googleGeoClient = new DefaultGoogleGeoClient(apiKey, geoServiceAccountPrivateKeyJson)) {
			for (InputAddress inputAddress : inputAddresses) {
				System.out.println(ToStringBuilder.reflectionToString(inputAddress, ToStringStyle.SHORT_PREFIX_STYLE));

				SearchTextRequest searchTextRequest = SearchTextRequest.newBuilder()
						.setTextQuery(format("%s %s", inputAddress.getName(), inputAddress.getAddress()))
						.setLanguageCode("en")
						.setRegionCode("US")
						//.setIncludedType("includedType-45971946")
						//.setOpenNow(true)
						//.setMinRating(-543315926)
						//.setMaxResultCount(-1736124056)
						//.addAllPriceLevels(new ArrayList<PriceLevel>())
						//.setStrictTypeFiltering(true)
						//.setLocationBias(SearchTextRequest.LocationBias.newBuilder().build())
						//.setLocationRestriction(SearchTextRequest.LocationRestriction.newBuilder().build())
						//.setEvOptions(SearchTextRequest.EVOptions.newBuilder().build())
						//.setRoutingParameters(RoutingParameters.newBuilder().build())
						//.setSearchAlongRouteParameters(SearchTextRequest.SearchAlongRouteParameters.newBuilder().build())
						.build();

				try {
					SearchTextResponse response = googleGeoClient.findPlacesBySearchText(searchTextRequest);

					if (response.getPlacesList().size() > 0) {
						//System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(response.getRawPlaces().get(0)));
						normalizedPlacesByInputAddressId.put(inputAddress.getId(), new NormalizedPlace(response.getPlacesList().get(0)));
					} else {
						// No match for name + address?  Then try just the address
						searchTextRequest = SearchTextRequest.newBuilder()
								.setTextQuery(inputAddress.getAddress())
								.setLanguageCode("en")
								.setRegionCode("US")
								.build();

						response = googleGeoClient.findPlacesBySearchText(searchTextRequest);

						if (response.getPlacesList().size() > 0) {
							//System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(response.getRawPlaces().get(0)));
							normalizedPlacesByInputAddressId.put(inputAddress.getId(), new NormalizedPlace(response.getPlacesList().get(0)));
						} else {
							// No match for just the address?  Then bail entirely.
							System.out.printf("Warning: no place data found for %s\n", ToStringBuilder.reflectionToString(inputAddress, ToStringStyle.SHORT_PREFIX_STYLE));
						}
					}
				} catch (Exception e) {
					System.err.printf("Error: failed to load place data for %s\n", ToStringBuilder.reflectionToString(inputAddress, ToStringStyle.SHORT_PREFIX_STYLE));
					e.printStackTrace();
				}

				++processedRows;

				System.out.printf("Processed %d of %d rows.\n", processedRows, inputAddresses.size());

				if (processedRows >= processedRowLimit) {
					System.out.printf("Hit processing limit of %d, stopping.\n", processedRowLimit);
					break;
				}
			}
		}

		// 3. Write the output file rows
		final List<String> OUTPUT_HEADERS = List.of(
				"id",
				"name",
				"address",
				"google_maps_url",
				"google_place_id",
				"latitude",
				"longitude",
				"premise",
				"subpremise",
				"formatted_address",
				"street_address_1",
				"street_address_2",
				"street_address_3",
				"locality",
				"region",
				"region_subdivision",
				"postal_code",
				"postal_code_suffix",
				"country_code"
		);

		try (Writer writer = new FileWriter(outputFile.toFile(), StandardCharsets.UTF_8);
				 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(OUTPUT_HEADERS.toArray(new String[0])))) {

			for (InputAddress inputAddress : inputAddresses) {
				List<String> recordElements = new ArrayList<>();
				recordElements.add(inputAddress.getId());
				recordElements.add(inputAddress.getName());
				recordElements.add(inputAddress.getAddress());

				NormalizedPlace normalizedPlace = normalizedPlacesByInputAddressId.get(inputAddress.getId());

				if (normalizedPlace != null) {
					// The hack to link to place ID via https://www.google.com/maps/place/?q=place_id:ChIJDY3f5HG4xokRblK1qaBaAXc only works sometimes...use the real URL provided by the API
					recordElements.add(normalizedPlace.getGoogleMapsUrl());
					recordElements.add(normalizedPlace.getGooglePlaceId());
					recordElements.add(String.valueOf(normalizedPlace.getLatitude()));
					recordElements.add(String.valueOf(normalizedPlace.getLongitude()));
					recordElements.add(normalizedPlace.getPremise());
					recordElements.add(normalizedPlace.getSubpremise());
					recordElements.add(normalizedPlace.getFormattedAddress());
					recordElements.add(normalizedPlace.getStreetAddress1());
					recordElements.add(normalizedPlace.getStreetAddress2());
					recordElements.add(normalizedPlace.getStreetAddress3());
					recordElements.add(normalizedPlace.getLocality());
					recordElements.add(normalizedPlace.getRegion());
					recordElements.add(normalizedPlace.getRegionSubdivision());
					recordElements.add(normalizedPlace.getPostalCode());
					recordElements.add(normalizedPlace.getPostalCodeSuffix());
					recordElements.add(normalizedPlace.getCountryCode());
				}

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		}
	}

	@NotThreadSafe
	protected static class InputAddress {
		@Nullable
		private String id;
		@Nullable
		private String name;
		@Nullable
		private String address;

		@Nullable
		public String getId() {
			return this.id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getAddress() {
			return this.address;
		}

		public void setAddress(@Nullable String address) {
			this.address = address;
		}
	}
}