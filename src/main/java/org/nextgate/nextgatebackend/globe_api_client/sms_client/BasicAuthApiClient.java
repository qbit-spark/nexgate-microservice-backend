package org.nextgate.nextgatebackend.globe_api_client.sms_client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class BasicAuthApiClient {

    private final WebClient webClient;

//    @Value("${api.sms-username}")
//    private  String userName;
//
//    @Value("${api.sms-password}")
//    private  String password;



    // POST request without authentication
    public <T> T postRequest(String url, Object request, Class<T> responseType, String someText) {
        return webClient
                .post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(responseType)
                .block(); // Blocking for synchronous processing
    }

    // GET request without authentication
    public <T> T getRequest(String url, Class<T> responseType) {
        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .block(); // Blocking for synchronous processing
    }

    // POST request with basic authentication
//    public <T> T postRequest(String url, Object request, Class<T> responseType) {
//        HttpHeaders headers = createBasicAuthHeaders(userName, password);
//
//        return webClient
//                .post()
//                .uri(url)
//                .headers(httpHeaders -> httpHeaders.addAll(headers)) // Add basic auth headers
//                .bodyValue(request)
//                .retrieve()
//                .bodyToMono(responseType)
//                .block(); // Blocking for synchronous processing
//    }



    public <T> Mono<T> uploadFiles(MultipartFile[] files, String folderName,String serverUrl, Class<T> responseType) {

        System.out.println("Upload Directory: " + serverUrl);
        System.out.println("User Folder Name: " + folderName);


        return webClient.post()
                .uri(serverUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                .bodyValue(createMultipartBody(files, folderName))
                .retrieve()
                .bodyToMono(responseType); // Return the response as Mono of type T
    }

    // Helper method to construct the multipart body
    private Object createMultipartBody(MultipartFile[] files, String folderName) {
        // Create a map for the form-data body
        return new MultipartRequestBody(files, folderName);
    }

    // Helper class to represent the multipart form-data
    public static class MultipartRequestBody {
        private final MultipartFile[] files;
        private final String folderName;

        public MultipartRequestBody(MultipartFile[] files, String folderName) {
            this.files = files;
            this.folderName = folderName;
        }

        // Getters, setters, and any other methods if needed
    }
    
    // Create basic authentication headers
    private HttpHeaders createBasicAuthHeaders(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        return headers;
    }
}
