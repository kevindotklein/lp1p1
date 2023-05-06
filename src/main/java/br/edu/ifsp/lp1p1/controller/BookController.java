package br.edu.ifsp.lp1p1.controller;

import br.edu.ifsp.lp1p1.dto.book.BookResponseDTO;
import br.edu.ifsp.lp1p1.dto.loan.LoanRequestDTO;
import br.edu.ifsp.lp1p1.mapper.book.BookResponseDTOMapper;
import br.edu.ifsp.lp1p1.model.Book;
import br.edu.ifsp.lp1p1.model.Loan;
import br.edu.ifsp.lp1p1.model.User;
import br.edu.ifsp.lp1p1.service.BookService;
import br.edu.ifsp.lp1p1.service.LoanService;
import br.edu.ifsp.lp1p1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/books")
@Log4j2
public class BookController {

    private final BookService bookService;
    private final LoanService loanService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<BookResponseDTO>> findAll(){
        List<BookResponseDTO> books = this.bookService.findAll();
        return ResponseEntity.ok(books);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        Book book = this.bookService.findById(id);
        this.loanService.deleteAllByBook(book);
        this.bookService.deleteById(book.getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{id}/loan")
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('EMPLOYEE')")
    public ResponseEntity<Void> createLoan(@RequestBody LoanRequestDTO loanRequestDTO,
                                           @PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails){
        log.info(userDetails.getUsername());
        log.info(userDetails.getAuthorities());
        Book book = this.bookService.findById(id);
        User client = this.userService.findById(loanRequestDTO.clientId());
        User user = this.userService.findByEmail(userDetails.getUsername());
        book.setNumberOfCopiesAvailable(book.getNumberOfCopiesAvailable()-1);
        this.bookService.save(book);
        this.userService.save(client);
        this.userService.save(user);

        Loan loan = new Loan().builder()
                .id(null)
                .book(book)
                .client(client)
                .user(user)
                .loanDate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toInstant(ZoneOffset.UTC))
                .returnDate(Instant.parse(loanRequestDTO.returnDate())).build();

        this.loanService.save(loan);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/{id}/return")
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('EMPLOYEE')")
    public ResponseEntity<BookResponseDTO> returnBook(@PathVariable Long id,
                                                      @RequestParam(required = true) Long clientId) {
        Book book = this.bookService.findById(id);

        List<Loan> loans = this.loanService.findAllByBook(book);
        if(loans.size() <= 0){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        for(Loan l : loans){
            if(l.getClient().getId() == clientId){
                this.loanService.deleteById(l.getId());
            }
        }

        book.setNumberOfCopiesAvailable(book.getNumberOfCopiesAvailable()+1);
        this.bookService.save(book);
        BookResponseDTO bookResponseDTO = BookResponseDTOMapper.INSTANCE.toBookResponseDTO(book);
        return ResponseEntity.ok(bookResponseDTO);
    }
}
