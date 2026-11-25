package org.wlcp.wlcpgameserver.controller.V2;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.wlcp.wlcpgameserver.dto.AutoReconnectDto;
import org.wlcp.wlcpgameserver.repository.GameInstanceRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequestMapping("/gameInstanceController/v2")
public class GameInstanceControllerV2 {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
	private GameInstanceRepository gameInstanceRepository;

    @GetMapping("/validateGamePin/{gamePin}")
    public ResponseEntity<Boolean> validateGamePin(@PathVariable int gamePin) { 
        return ResponseEntity.ok().body(gameInstanceRepository.findById(gamePin).isPresent());
    }

    @GetMapping("/checkForAutoReconnect")
    public ResponseEntity<AutoReconnectDto> checkForAutoReconnect(HttpServletRequest request, HttpServletResponse response) throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException {
        // Retrieve cookies from the request
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("lastConnectedGameInstance".equals(cookie.getName())) {
                    AutoReconnectDto autoReconnectDto = objectMapper.readValue(URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8.name()), AutoReconnectDto.class);
                    if(gameInstanceRepository.findById(Integer.valueOf(autoReconnectDto.gameInstanceId)).isPresent()) {
                        return ResponseEntity.ok(autoReconnectDto);
                    } else {
                        Cookie gameSessionCookie = new Cookie("lastConnectedGameInstance", null);
                        gameSessionCookie.setMaxAge(0); // Cookie expires after 24 hours
                        gameSessionCookie.setPath("/"); // Available for the entire application
                        gameSessionCookie.setHttpOnly(true); // Optional: makes the cookie not accessible via JavaScript (for security)
                        
                        // Add the cookie to the response
                        response.addCookie(gameSessionCookie);
                        return ResponseEntity.ok().body(new AutoReconnectDto());
                    }
                }
            }
        }
        return ResponseEntity.ok().body(new AutoReconnectDto());
    }

    @PostMapping("/setupAutoReconnect")
    public ResponseEntity<AutoReconnectDto> setupAutoReconnect(@RequestBody AutoReconnectDto entity, HttpServletResponse response) throws JsonProcessingException, UnsupportedEncodingException {
        
        Cookie gameSessionCookie = new Cookie("lastConnectedGameInstance", URLEncoder.encode(objectMapper.writeValueAsString(entity), StandardCharsets.UTF_8.name()));
        gameSessionCookie.setMaxAge(60 * 60 * 24); // Cookie expires after 24 hours
        gameSessionCookie.setPath("/"); // Available for the entire application
        gameSessionCookie.setHttpOnly(true); // Optional: makes the cookie not accessible via JavaScript (for security)
        
        // Add the cookie to the response
        response.addCookie(gameSessionCookie);
        
        return ResponseEntity.ok(entity);
    }    
    
    
}
