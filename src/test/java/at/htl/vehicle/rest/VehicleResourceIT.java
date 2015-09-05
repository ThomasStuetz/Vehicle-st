package at.htl.vehicle.rest;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.IsNot.not;

public class VehicleResourceIT {

    private Client client;
    private WebTarget target;

    @Before
    public void initClient() {
        this.client = ClientBuilder.newClient();
        this.target = client.target("http://localhost:8080/vehicle/rs/vehicle");
    }

    @Test
    public void crud() {
        JsonObjectBuilder vehicleBuilder = Json.createObjectBuilder();
        JsonObject vehicleToCreate = vehicleBuilder
                .add("brand", "Opel")
                .add("type", "Commodore")
                .build();

        // create
        Response postResponse = this.target
                .request().post(Entity.json(vehicleToCreate));
        assertThat(postResponse.getStatus(), is(201));
        String location = postResponse.getHeaderString("Location");
        System.out.println("location = " + location);

        // find
        JsonObject dedicatedVehicle = this.client
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .get(JsonObject.class);
        assertThat(dedicatedVehicle.getString("brand"), containsString("Opel"));
        assertThat(dedicatedVehicle.getString("type"), equalTo("Commodore"));

        // update
        JsonObjectBuilder updateBuilder = Json.createObjectBuilder();
        JsonObject updated = updateBuilder
                .add("brand", "Opel")
                .add("type", "Kapitän")
                .build();

        Response updateResponse = this.client
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(updated));
        assertThat(updateResponse.getStatus(), is(200));

        // update again
        updateBuilder = Json.createObjectBuilder();
        updated = updateBuilder
                .add("brand", "VW")
                .add("type", "Käfer 1400")
                .build();

        updateResponse = this.client
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(updated));
        assertThat(updateResponse.getStatus(), is(409));
        String conflictInformation = updateResponse.getHeaderString("cause");
        assertThat(conflictInformation, not(isEmptyString()));
        System.out.println("conflictInformation = " + conflictInformation);


        // find it again
        JsonObject updatedVehicle = this.client
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .get(JsonObject.class);
        assertThat(updatedVehicle.getString("brand"), equalTo("Opel"));
        assertThat(updatedVehicle.getString("type"), equalTo("Kapitän"));

        // update vignetteValid
        JsonObjectBuilder vignetteValidBuilder = Json.createObjectBuilder();
        JsonObject vignetteValid = updateBuilder
                .add("annualVignetteValid", true)
                .build();

        this.client
                .target(location)
                .path("vignette_valid")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(vignetteValid));

        // verify vignetteValid
        updatedVehicle = this.client
                .target(location)
                .request(MediaType.APPLICATION_JSON)
                .get(JsonObject.class);
        assertThat(updatedVehicle.getBoolean("annualVignetteValid"), equalTo(true));


        // update not existing  vignetteValid
        JsonObjectBuilder notExistingBuilder = Json.createObjectBuilder();
        vignetteValid = notExistingBuilder
                .add("annualVignetteValid", true)
                .build();

        Response response = this.target
                .path("-42")
                .path("vignette_valid")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(vignetteValid));
        assertThat(response.getStatus(), is(400));
        assertThat(response.getHeaderString("reason"), not(isEmptyString()));

        // update malformed vignetteValid
        notExistingBuilder = Json.createObjectBuilder();
        vignetteValid = notExistingBuilder
                .add("something wrong", true)
                .build();

        response = this.client
                .target(location)
                .path("vignette_valid")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.json(vignetteValid));
        assertThat(response.getStatus(), is(400));
        assertThat(response.getHeaderString("reason"), not(isEmptyString()));

        // findAll
        response = this.target
                .request(MediaType.APPLICATION_JSON)
                .get();
        assertThat(response.getStatus(), is(200));
        JsonArray allTodos = response.readEntity(JsonArray.class);
        System.out.println("payload = " + allTodos);
        assertThat(allTodos, not(empty()));

        JsonObject vehicle = allTodos.getJsonObject(0);
        assertThat(vehicle.getString("brand"), equalTo("Opel"));

        // deleting not-existing
        Response deleteResponse = this.target
                .path("42")
                .request(MediaType.APPLICATION_JSON)
                .delete();
        assertThat(deleteResponse.getStatus(), is(204)); // no content
    }

    @Test
    public void createVehicleWithoutBrand() {
        JsonObjectBuilder vehicleBuilder = Json.createObjectBuilder();
        JsonObject vehicleToCreate = vehicleBuilder
                .add("type", "Commodore")
                .build();

        Response postResponse = this.target
                .request()
                .post(Entity.json(vehicleToCreate));
        assertThat(postResponse.getStatus(),is(400));
        String location = postResponse.getHeaderString("Location");
        System.out.println("location = " + location);
        postResponse.getHeaders().entrySet().forEach(System.out::println);
    }

    @Test
    public void createValidVehicle() {
        JsonObjectBuilder vehicleBuilder = Json.createObjectBuilder();
        JsonObject vehicleToCreate = vehicleBuilder
                .add("brand", "VW")
                .add("type", "Käfer 1400")
                .build();

        Response postResponse = this.target
                .request()
                .post(Entity.json(vehicleToCreate));
        assertThat(postResponse.getStatus(),is(201));
        String location = postResponse.getHeaderString("Location");
        System.out.println("location = " + location);
    }
}
