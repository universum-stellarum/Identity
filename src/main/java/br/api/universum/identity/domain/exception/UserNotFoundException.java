package br.api.universum.identity.domain.exception;

/** Indicates that the requested user does not exist. */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException() {
        super("User not found.");
    }
}
