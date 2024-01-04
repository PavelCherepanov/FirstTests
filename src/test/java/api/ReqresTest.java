package api;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Test;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

public class ReqresTest {
    private final static String URL = "https://reqres.in/";

    @Test
    public void checkAvatarAndIdWithoutPojo() {
        Specifications.installSpecification(Specifications.requestSpecification(URL),
                Specifications.responseSpecificationUnique(200));
        Response response = given()
                .when()
                .get("api/users?page=2")
                .then().log().all()
                .body("page", equalTo(2))
                .body("data.id", notNullValue())
                .extract().response();
        JsonPath jsonPath = response.jsonPath();
        List<String> emails = jsonPath.get("data.email");
        List<Integer> ids = jsonPath.get("data.id");
        List<String> avatars = jsonPath.get("data.avatar");
        for (int i = 0; i < avatars.size(); i++) {
            Assert.assertTrue(avatars.get(i).contains(ids.get(i).toString()));
        }

        Assert.assertTrue(emails.stream().allMatch(x -> x.endsWith("@reqres.in")));

    }

    @Test
    public void successUserWithoutPojo() {
        Specifications.installSpecification(Specifications.requestSpecification(URL),
                Specifications.responseSpecificationUnique(200));
        Map<String, String> user = new HashMap<>();
        user.put("email", "eve.holt@reqres.in");
        user.put("password", "pistol");
//        given()
//                .body(user)
//                .when()
//                .post("api/register")
//                .then().log().all()
//                .body("id", equalTo(4))
//                .body("token", equalTo("QpwL5tke4Pnpja7X4"));
        Response response = given()
                .body(user)
                .when()
                .post("api/register")
                .then().log().all()
                .extract().response();
        JsonPath jsonPath = response.jsonPath();
        Assert.assertEquals(4, (int) jsonPath.get("id"));
        Assert.assertEquals("QpwL5tke4Pnpja7X4", jsonPath.get("token"));
    }

    @Test
    public void unSuccessUserWithoutPojo() {
        Specifications.installSpecification(Specifications.requestSpecification(URL),
                Specifications.responseSpecificationUnique(400));
        Map<String, String> user = new HashMap<>();
        user.put("email", "sydney@fife");
        user.put("password", "");
//        given()
//                .body(user)
//                .when()
//                .post("api/register")
//                .then().log().all()
//                .body("error", equalTo("Missing password"));
        Response response = given()
                .body(user)
                .when()
                .post("api/register")
                .then().log().all()
                .extract().response();
        JsonPath jsonPath = response.jsonPath();
        Assert.assertEquals("Missing password", jsonPath.get("error"));

    }

    @Test
    public void checkAvatarAndId() {
        Specifications.installSpecification(Specifications.requestSpecification(URL), Specifications.responseSpecification200());
        List<UserData> users = given()
                .when()
//                .contentType(ContentType.JSON)
//                .get(URL + "api/users?page=2")
                .get("api/users?page=2")
                .then().log().all()
                .extract().body().jsonPath().getList("data", UserData.class);
        users.forEach(x -> Assert.assertTrue(x.getAvatar().contains(x.getId().toString())));


        List<String> avatars = users.stream().map(UserData::getAvatar).toList();
        List<String> ids = users.stream().map(x -> x.getId().toString()).toList();

        for (int i = 0; i < avatars.size(); i++) {
            Assert.assertTrue(avatars.get(i).contains(ids.get(i)));
        }
    }

    @Test
    public void checkEmail() {
        List<UserData> users = given()
                .when()
                .contentType(ContentType.JSON)
                .get(URL + "api/users?page=2")
                .then().log().all()
                .extract().body().jsonPath().getList("data", UserData.class);
        Assert.assertTrue(users.stream().allMatch(x -> x.getEmail().endsWith("@reqres.in")));
    }

    @Test
    public void successReg() {
        Specifications.installSpecification(Specifications.requestSpecification(URL), Specifications.responseSpecification200());
        Integer id = 4;
        String token = "QpwL5tke4Pnpja7X4";
        Register user = new Register("eve.holt@reqres.in", "pistol");
        SuccessRegistration successRegistration = given()
                .body(user)
                .when()
                .post("api/register")
                .then().log().all()
                .extract().as(SuccessRegistration.class);


        Assert.assertNotNull(successRegistration.getId());
        Assert.assertNotNull(successRegistration.getToken());
        Assert.assertEquals(id, successRegistration.getId());
        Assert.assertEquals(token, successRegistration.getToken());
    }

    @Test
    public void unSuccessReg() {
        Specifications.installSpecification(Specifications.requestSpecification(URL), Specifications.responseSpecification400());
        Register user = new Register("sydney@fife", "");
        UnSuccessReg unSuccessReg = given()
                .body(user)
                .when()
                .post("api/register")
                .then().log().all()
                .extract().as(UnSuccessReg.class);
        Assert.assertEquals("Missing password", unSuccessReg.getError());
    }

    @Test
    public void sortedYears() {
        Specifications.installSpecification(Specifications.requestSpecification(URL), Specifications.responseSpecification200());
        List<ColorsData> colors = given()
                .when()
                .get("api/unknown")
                .then().log().all()
                .extract().body().jsonPath().getList("data", ColorsData.class);

        List<Integer> years = colors.stream().map(ColorsData::getYear).toList();
        List<Integer> sortedYear = years.stream().sorted().toList();
        Assert.assertEquals(sortedYear, years);
    }

    @Test
    public void deleteUser() {
        Specifications.installSpecification(Specifications.requestSpecification(URL), Specifications.responseSpecificationUnique(204));
        given()
                .when()
                .delete("api/delete/2")
                .then().log().all();
    }

    @Test
    public void checkTime() {
        Specifications.installSpecification(Specifications.requestSpecification(URL), Specifications.responseSpecificationUnique(200));
        UserTime user = new UserTime("morpheus", "zion resident");//2024-01-03T12:33:59.640Z"
        UserTimeResponse response = given()
                .body(user)
                .when()
                .put("api/users/2")
                .then().log().all()
                .extract().as(UserTimeResponse.class);
        String regex = "(.{5}$)";
        String currentTime = Clock.systemUTC().instant().toString().replaceAll(regex, "");
        Assert.assertEquals(currentTime.replaceAll("(.{6}$)", ""), response.getUpdatedAt().replaceAll(regex, ""));
    }


}
