package de.yanwittmann.upload;

import de.yanwittmann.bingo.BingoBoard;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class BingoDatabaseInterface {

    private final String baseUrl;

    public BingoDatabaseInterface(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/$", "") + "/";
    }

    public BingoDatabaseInterface(URL baseUrl) {
        this.baseUrl = baseUrl.toString().replaceAll("/$", "") + "/";
    }

    public JSONObject upload(BingoBoard board, boolean allowMultipleClaims) throws IOException {
        Map<String, String> postData = new HashMap<>();
        JSONObject boardJson = board.toJson();
        boardJson.remove("categories");
        postData.put("boardJson", boardJson.toString());
        postData.put("allow_multiple_claims", allowMultipleClaims ? "1" : "0");
        return new JSONObject(apiCall("create-board.php", postData));
    }

    public JSONObject delete(int boardId) throws IOException {
        Map<String, String> postData = new HashMap<>();
        postData.put("boardId", String.valueOf(boardId));
        return new JSONObject(apiCall("delete-board.php", postData));
    }

    public JSONObject getBoardTiles(int boardId) throws IOException {
        Map<String, String> postData = new HashMap<>();
        postData.put("boardId", String.valueOf(boardId));
        return new JSONObject(apiCall("get-board-tiles.php", postData));
    }

    public JSONObject claimBoardTile(int boardId, int x, int y, char claimer) throws IOException {
        Map<String, String> postData = new HashMap<>();
        postData.put("boardId", String.valueOf(boardId));
        postData.put("x", String.valueOf(x));
        postData.put("y", String.valueOf(y));
        postData.put("claim", String.valueOf(claimer));
        return new JSONObject(apiCall("claim-board-tile.php", postData));
    }

    public JSONObject getBoardClaims(int boardId) throws IOException {
        Map<String, String> postData = new HashMap<>();
        postData.put("boardId", String.valueOf(boardId));
        return new JSONObject(apiCall("get-board-claims.php", postData));
    }

    public JSONArray getBoards() throws IOException {
        Map<String, String> postData = new HashMap<>();
        return new JSONArray(apiCall("get-boards.php", postData));
    }

    private String apiCall(String file, Map<String, String> postData) throws IOException {
        URL url = new URL(baseUrl + file);
        System.out.println("Performing request to " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        // write post data
        StringJoiner postDataString = new StringJoiner("&");
        postData.forEach((key, value) -> postDataString.add(key + "=" + value));
        connection.getOutputStream().write(postDataString.toString().getBytes());
        connection.getOutputStream().flush();
        connection.getOutputStream().close();
        // read response
        StringBuilder response = new StringBuilder();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            // input stream is an array of bytes
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                response.append(new String(buffer, 0, bytesRead));
            }
            inputStream.close();
        }
        try {
            System.out.println("Response:\n" + new JSONObject(response.toString()).toString(2));
        } catch (Exception ignored) {
            System.out.println("Response: " + response);
        }
        return response.toString();
    }
}
