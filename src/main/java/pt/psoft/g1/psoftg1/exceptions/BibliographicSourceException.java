package pt.psoft.g1.psoftg1.exceptions;

public class BibliographicSourceException extends RuntimeException {

    public BibliographicSourceException(String message) {
        super(message);
    }

    public BibliographicSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
