package br.edu.ifsp.lp1p1.dto.book;

public record BookResponseDTO(Long id,
                              String title,
                              String author,
                              String publisher,
                              String publicationYear,
                              Integer numberOfCopies,
                              Integer numberOfCopiesAvailable) {
}
