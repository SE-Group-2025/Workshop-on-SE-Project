package com.SEGroup.Service;

import javax.naming.AuthenticationException;

import org.springframework.beans.factory.annotation.Autowired;

import com.SEGroup.Domain.IAuthenticationService;
import com.SEGroup.Infrastructure.PasswordEncoder;
import com.SEGroup.Infrastructure.Security;

import org.springframework.stereotype.Component;
/**
 * SecurityAdapter class implements the IAuthenticationService interface.
 * It provides an adapter to interact with the Security and PasswordEncoder services
 * for user authentication, password encryption, session validation, and token generation.
 */
@Component
public class SecurityAdapter implements IAuthenticationService {


    public SecurityAdapter(Security sec, PasswordEncoder passwordEncoder) {
        this.sec = sec;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    Security sec;  // Instance of the Security class to handle JWT operations

    @Autowired
    PasswordEncoder passwordEncoder;  // Instance of PasswordEncoder for password encryption and verification
    public SecurityAdapter(){
        passwordEncoder = new PasswordEncoder();
    }
    /**
     * Checks if the provided session key (JWT token) is valid.
     * Throws an AuthenticationException if the token is invalid.
     *
     * @param sessionKey The session key (JWT token) to check.
     * @throws AuthenticationException If the token is invalid.
     */
    @Override
    public void checkSessionKey(String sessionKey) throws AuthenticationException {
        if (!sec.validateToken(sessionKey)) {
            throw new AuthenticationException("Token is Invalid!");
        }
    }

    /**
     * Invalidates the session by making the provided session key (JWT token) expire.
     *
     * @param sessionKey The session key (JWT token) to invalidate.
     * @throws AuthenticationException If the session key cannot be invalidated.
     */
    @Override
    public void invalidateSession(String sessionKey) throws AuthenticationException {
        sec.makeTokenExpire(sessionKey);
    }

    /**
     * Retrieves the user email associated with the provided session key (JWT token).
     *
     * @param sessionKey The session key (JWT token) to retrieve the user email from.
     * @return The email of the user associated with the session.
     * @throws AuthenticationException If the token is invalid or cannot be parsed.
     */
    @Override
    public String getUserBySession(String sessionKey) throws AuthenticationException {
        String userEmail = sec.extractUsername(sessionKey);
        if (userEmail == null) {
            throw new AuthenticationException("Token is invalid!");
        }
        return userEmail;
    }

    /**
     * Authenticates a user by generating a JWT token for the provided email.
     *
     * @param email The email of the user to authenticate.
     * @return A JWT token representing the authenticated user.
     */
    @Override
    public String authenticate(String email) {
        return sec.generateToken(email);
    }


    /**
     * Encrypts the provided password using the PasswordEncoder service.
     *
     * @param password The password to encrypt.
     * @return The encrypted password.
     */
    @Override
    public String encryptPassword(String password) {
        return passwordEncoder.encrypt(password);
    }

    /**
     * Compares the provided real password with the encrypted password to check for a match.
     * Throws an AuthenticationException if the passwords do not match.
     *
     * @param encryptedPassword The encrypted password stored in the system.
     * @param realPassword The plain-text password entered by the user.
     * @throws AuthenticationException If the passwords do not match.
     */
    @Override
    public void matchPassword(String encryptedPassword, String realPassword) throws AuthenticationException {
        if (!passwordEncoder.checkPassword(realPassword, encryptedPassword)) {
            throw new AuthenticationException("Wrong password.");
        }
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

}