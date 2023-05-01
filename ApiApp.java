package cs1302.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import cs1302.api.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.geometry.*;
import javafx.scene.text.Text;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.event.ActionEvent;
import java.util.HashSet;
import javafx.event.EventHandler;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import java.time.LocalTime;
import javafx.util.Duration;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * REPLACE WITH NON-SHOUTING DESCRIPTION OF YOUR APP.
 */
public class ApiApp extends Application {

    private static class Movie {
        String tmsId;
        String title;
    }

    private static class TMovieResponse {
        int page;
        TMovie [] results;
    }

    private static class TMovie {
        String original_title;
        String id;
        String backdrop_path;
    }





   /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

/** Google {@code Gson} object for parsing JSON-formatted strings. */

    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                        // enable nice output when printing
        .create();                                  // builds and returns a Gson object

    Stage stage;
    Scene scene;
    VBox root;
    HBox titleLayer;
    HBox showLayer;
    String imageUrl;
    Date date;

    HBox queryLayer;
    TextField searchField;



   /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        root = new VBox();
        date = new Date();
        imageUrl = "https://image.tmdb.org/t/p/original";

        titleLayer = new HBox(8);
        queryLayer = new HBox(8);

        searchField = new TextField();
        searchField.setPromptText("zip code:");


    } // ApiApp

    public void init() {
        this.root.getChildren().addAll(titleLayer, queryLayer);
        queryLayer.getChildren().addAll(searchField);

        Image bannerImage = new Image("https://static.vecteezy.com/system/resources/"
            + "previews/011/834/992/original/blank-ticket-template-png.png");
        ImageView banner = new ImageView(bannerImage);
        banner.setPreserveRatio(true);
        banner.setFitWidth(640);

        // some labels to display information


        // setup scene
        root.getChildren().addAll(banner);
    }


    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        String newDate = new SimpleDateFormat("yyyy-MM-dd").format(date);

        String url = "https://data.tmsapi.com/v1.1/movies/showings?api_key="
            + "fvekujkhpc7zxt7rtmx77gvr&startDate=" + newDate + "&zip=30605";

        String body = this.connect(url);
        String title = this.parse(body);
        // demonstrate how to load local asset using "file:resources/"
        this.getMovieReview(title);


        scene = new Scene(root);

        // setup stage
        stage.setTitle("ApiApp!");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.sizeToScene();
        stage.show();

    } // start

     /**
      * Returns the request as String formatted in JSON.
      * @param url the url
      * @return search as the response body.
      */
    public static String connect(String url) {
        String body = "";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .uri(URI.create(url))
                .headers("Accept-Enconding", "gzip, deflate")
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }

            // use response body
            body =  response.body();
            System.out.println("body :" + body);
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
            e.printStackTrace();
        }
        return body;
    }

    /**
     * Parses the JSON output and assigns non-duplicate URIs to a String array.
     * @param body the response body of JSON
     * @return String array of non-duplicate URIs
     */
    public String parse(String body) {
        Movie [] movie  = GSON.fromJson(body, Movie[].class);
        try {
            for (int i = 0; i < movie.length; i++) {
                String title = movie[i].title;
                System.out.println("title : " + title);
                return title;
            }
        } catch (Exception e) {
            TextArea text =
                new TextArea("Exception: " + e );
            text.setEditable(false);
            Alert alert = new Alert(AlertType.ERROR);
            alert.getDialogPane().setPrefSize(377, 233);
            alert.getDialogPane().setContent(text);
            alert.setResizable(true);
            alert.showAndWait();
        } // try-catch

        return null;
    }

    public String  getMovieReview(String title) {

        String ntitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String body = this.connect("https://api.themoviedb.org/3/search/movie?api_key="
            + "7df71a299c6ebb48c7ed1e01eb9e174b&query=" + ntitle);

        TMovieResponse apiResponse = GSON.fromJson(body,TMovieResponse.class);

        String id = apiResponse.results[0].id;
        String picURL = apiResponse.results[0].backdrop_path;

        String reviewBody = this.connect("https://api.themoviedb.org/3/movie/" + id +"/reviews?"
        + "api_key=7df71a299c6ebb48c7ed1e01eb9e174b");

        System.out.print("reviewBody : \n" + reviewBody);

        return reviewBody;
    }

} // ApiApp
