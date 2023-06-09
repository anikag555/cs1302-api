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
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.scene.text.*;
import java.util.HashMap;
import java.lang.Math;
import com.google.gson.annotations.SerializedName;

/**
 * REPLACE WITH NON-SHOUTING DESCRIPTION OF YOUR APP.
 */
public class ApiApp extends Application {

    /**
     * Represents response from GraceNote API, used to query and retrieve
     * data of currently playing movies in theaters like titles,showtimes, and posters.
     */
    private static class Movie {
        String tmsId;
        String title;
        PreferredImage preferredImage;
        Showtime [] showtimes;
        String [] topCast;
        String [] directors;
        String [] genres;
    }

    /**
     * This represents the URL of currently playing movie poster image.
     */
    private static class PreferredImage {
        String uri;
    }

    /**
     * This represents the current playing movie showtimes in theaters.
     */
    private static class Showtime {
        Theater theatre;
        String dateTime;
        String ticketURI;
    }

    /**
     * This represents theater information of the current playing movie.
     */
    private static class Theater {
        String id;
        String name;
    }

    /**
     * This represents the reponse from the TMDB API for a given movie selected by the user.
     */
    private static class TMovieResponse {
        int page;
        TMovie [] results;
    }

    /**
     * This represents the title of movie selected by the user
     * and its respective movie identification for the TMDB API.
     */
    private static class TMovie {
        @SerializedName("original_title")
        String orginalTitle;
        String id;
    }

    /**
     * This represents the results of reviews as the response from the TMDB API for
     * the movie selected by the user.
     */
    private static class TReviewsResponse {
        String id;
        String page;
        Review [] results;
    }

    /**
     * This represents the review data from the TMDB API for the movie selected by the user.
     */
    private static class Review {
        String author;
        @SerializedName("author_details")
        AuthorDetails authorDetails;
        String content;
    }

    /**
     * This represents the author of the
     * review data from the TMDB API for the movie selected by the user.
     */
    private static class AuthorDetails {
        String username;
        @SerializedName("avatar_path")
        String avatarPath;
        String rating;
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
    HBox queryLayer;
    TilePane tilePane;

    String posterUrl;
    Date date;
    String [] posterUrls;
    String reviewBody;

    Label searchLabel;
    TextField searchField;
    Button searchButton;
    HashMap<String, Movie> moviesMap;


   /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        // main components for scene
        root = new VBox();
        queryLayer = new HBox(8);

        date = new Date();
        posterUrl = "https://demo.tmsimg.com/";
        posterUrls = null;
        reviewBody = "";
        moviesMap = new HashMap<String,Movie>();

        // query layer components to handle query
        searchLabel = new Label ("Zip Code:");
        searchField = new TextField();
        searchButton = new Button();
        searchButton.setText("Movies");

        // to display currently playing movies
        tilePane = new TilePane();

    } // ApiApp

    /** {@inheritDoc} */
    @Override
    public void init() {
        // title layer
        Image bannerImage = new Image ("file:resources/title1.png");
        ImageView banner = new ImageView(bannerImage);
        banner.setPreserveRatio(true);
        banner.setFitWidth(720);

        // sets the appropriate HBoxes for root (VBox)
        this.root.getChildren().addAll(queryLayer, banner, tilePane);
        this.root.setVgrow(tilePane, Priority.ALWAYS);
        HBox.setHgrow(searchLabel,Priority.ALWAYS);
        HBox.setHgrow(searchField,Priority.ALWAYS);
        HBox.setHgrow(searchButton,Priority.ALWAYS);

        // sets appropriate components for HBox that takes query
        queryLayer.getChildren().addAll(searchLabel, searchField, searchButton);
        searchButton.setOnAction(queryHandler);

    } // init

    // Action Event to handle queries when Search Button is pressed
    EventHandler<ActionEvent> queryHandler = (ActionEvent e) -> {
        String search = searchField.getText();
        String filterSearch = URLEncoder.encode(search, StandardCharsets.UTF_8);
        String validSearch = "";

        // checks for valid input and passes input for URL that builds request
        try {
            if (filterSearch.length() == 5 ) {
                if (isInt(filterSearch)) {
                    validSearch = filterSearch;
                } else {
                    throw new NumberFormatException();
                }
            } else {
                throw new IllegalArgumentException();
            }
            String newDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
            String zip = validSearch;
            String key1 = "pvk4aug6w8ntdmhftwgpeasb";
            String key2 = "fvekujkhpc7zxt7rtmx77gvr";

            int temp = (int)Math.round(Math.random());
            String url = "";
            if (temp == 0) {
                url = "https://data.tmsapi.com/v1.1/movies/showings?api_key="
                    + key1 + "&startDate=" + newDate + "&zip=" + zip;
            } else {
                url = "https://data.tmsapi.com/v1.1/movies/showings?api_key="
                    + key2 + "&startDate=" + newDate + "&zip=" + zip;
            }

            this.tilePane.getChildren().clear();
            String body = this.connect(url);
            if (body.equals("")) {
                throw new Exception("Invalid zip code!");
            }
            this.parse(body);

        } catch (Exception nfe) {
            TextArea text =
                new TextArea("Error! Invalid input. Try again! \nException: " + nfe );
            text.setEditable(false);
            Alert alert = new Alert(AlertType.ERROR);
            alert.getDialogPane().setPrefSize(377, 233);
            alert.getDialogPane().setContent(text);
            alert.setResizable(true);
            alert.showAndWait();
        }
    };

    /**
     * Helper method for input validation that
     * accepts a String and return true if it can be parsed as an int.
     * @param s the zip code as a String
     * @return res true if entered input is String of numbers.
     */
    public static boolean isInt(String s) {
        boolean res = false;
        try {
            Integer.parseInt(s);
            res = true;
        } catch (NumberFormatException e) {
            res = false;
        }
        return res;
    } // isInt

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        scene = new Scene(root, 720, 720);

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
     * @return body as the response body.
     */
    public static String connect(String url) {
        String body = "";
        int responseCode = 0;
        try {
            // builds request and creates URI
            HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .uri(URI.create(url))
                .headers("Accept-Enconding", "gzip, deflate")
                .build();

            // sends request
            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());

            // checks status of resopnse -> if valid
            responseCode = response.statusCode();
            // checks for max queries
            if (responseCode == 403) {
                throw new IOException("Reached the daily free API limit for queries!"
                + "Try the search again for alternate API key!");
            } else if (responseCode == 200 ) {
                body =  response.body();
            } else {
                throw new IOException("HTTP " + responseCode);
            }

        } catch (IOException | InterruptedException e) {
            TextArea text =
                new TextArea("Exception: " + e );
            text.setEditable(false);
            Alert alert = new Alert(AlertType.ERROR);
            alert.getDialogPane().setPrefSize(377, 233);
            alert.getDialogPane().setContent(text);
            alert.setResizable(true);
            alert.showAndWait();
        }

        return body;
    } // connect

    /**
     * Parses the JSON output to display currently playing movies.
     * @param body the response body of GraceNote API
     */
    public void parse(String body) {
        final Movie [] movie  = GSON.fromJson(body, Movie[].class);
        try {
            // gathers uris for posters
            posterUrls = new String [movie.length];
            for (int i = 0; i < movie.length; i++) {
                String fullUrl = posterUrl + movie[i].preferredImage.uri;
                posterUrls [i] = fullUrl;
                moviesMap.put(movie[i].title,movie[i]);
            }
            tilePane.setOrientation(Orientation.HORIZONTAL);
            tilePane.setTileAlignment(Pos.CENTER_LEFT);
            tilePane.setPrefRows(4);
            Image [] allImages = new Image [posterUrls.length];
            // uploads posters of currently playing movies to TilePane
            for (int i = 0; i < posterUrls.length; i++) {
                allImages [i] = new Image (posterUrls[i]);
                ImageView imgView = new ImageView();
                imgView.setImage(allImages[i]);
                imgView.setFitWidth(100);
                imgView.setFitHeight(100);
                final String temp = movie[i].title;
                // when user presses on a movie, produces Movie Review
                imgView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    Popup a = new Popup();
                    VBox vBox = new VBox();
                    vBox.setStyle("-fx-padding: 10;" + "-fx-border-style: solid inside;" +
                        "-fx-border-width: 2; -fx-border-insets: 5; -fx-border-color: cadetblue;");
                    a.setAutoHide(true);
                    a.setAutoFix(true);
                    String movieShowInfo = this.getMovieShowInfo(temp);
                    Text text = new Text("\n\n TITLE:\n" + temp + "\n"
                        + movieShowInfo + "\n REVIEWS:\n" + this.getMovieReview(temp) + "\n\n");
                    text.setWrappingWidth(550);
                    text.setTextAlignment(TextAlignment.JUSTIFY);
                    // adds vBox to scrollPane
                    ScrollPane scrollPane = new ScrollPane(vBox);
                    vBox.getChildren().add(text);
                    scrollPane.setMaxHeight(600);
                    scrollPane.setMaxWidth(600);
                    // adds scrollPane to popup
                    a.getContent().add(scrollPane);
                    a.show(stage);
                    event.consume();
                });
                tilePane.getChildren().add(imgView);
            } // Mouse Event
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
    } // parse

    /**
     * After user presses a title in query handler,
     * uses title to send request to TMDB API and parse JSON output for Movie reviews
     * and prints the first 10 reviews.
     * @param title the selected movie's title.
     * @return reviewData the movie's reviews.
     */
    public String getMovieReview(String title) {
        String reviewData = "";
        try {
            String ntitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            // takes title to get movie ID
            String body = this.connect("https://api.themoviedb.org/3/search/movie?api_key="
                + "7df71a299c6ebb48c7ed1e01eb9e174b&query=" + ntitle);

            TMovieResponse apiResponse = GSON.fromJson(body,TMovieResponse.class);
            if (apiResponse.results.length == 0) {
                return "Reviews not found for this movie yet.";
            } // if-else
            String id = apiResponse.results[0].id;

            // takes movie ID and returns review data
            reviewBody = this.connect("https://api.themoviedb.org/3/movie/"
            + id + "/reviews?" + "api_key=7df71a299c6ebb48c7ed1e01eb9e174b");

            TReviewsResponse revResponse  = GSON.fromJson(reviewBody, TReviewsResponse.class);
            if (revResponse.results.length == 0) {
                return "Reviews not found for this movie yet.";
            } else {
                int x = 0;
                for (Review r : revResponse.results) {
                    // counter to print the first 10 reviews
                    if (x++ == 10) {
                        return reviewData;
                    } else {
                        reviewData += "Review #" + x + "\n"
                            + r.content + "\n----------------------------- \n ";
                    } // if-else
                } // for
            } // if-else
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
        return reviewData;
    } // getMovieReview


     /**
      * After user presses a title in query handler,
      * uses title to parse GraceNote API's JSON output for showtime data
      * and prints the selected movie's showtimes and related data.
      * @param title the selected movie's title.
      * @return data the movie's showtimes and other data.
      */
    public String getMovieShowInfo(String title) {
        String data = "";
        Movie mov = moviesMap.get(title);
        // movie casting
        if (mov.topCast != null) {
            data = data + "\nCASTING: \n";
            for (String x:mov.topCast) {
                data = data + x + "\n";
            }
        }
        // movie directors
        if (mov.directors != null) {
            data = data + "\nDIRECTOR: \n";
            for (String x:mov.directors) {
                data = data + x + "\n";
            }
        }
        // movie genres
        if (mov.genres != null) {
            data = data + "\nGENRE: \n";
            for (String x:mov.genres) {
                data = data + x + "\n";
            }
        }
        // movies showtimes
        if (mov.showtimes != null) {
            data = data + "\nSHOWTIMES: \n";
            for (Showtime sh:mov.showtimes) {
                if (sh.theatre != null) {
                    data += sh.theatre.name + "\n" + sh.dateTime + "\n"
                        + sh.ticketURI + "\n";
                }
            }
        }
        return data;
    } // getMovieShowInfo

} // ApiApp
