package com.rll.microservices.books;

import com.rll.microservices.books.dao.BookDAO;
import com.rll.microservices.books.model.Book;
import com.rll.microservices.common.model.authors.AuthorDTO;
import com.rll.microservices.common.model.authors.GetAuthorResponse;
import com.rll.microservices.common.model.books.BookDTO;
import com.rll.microservices.common.model.books.GetBookResponse;
import com.rll.microservices.common.model.books.GetBooksResponse;
import com.rll.microservices.common.model.operations.OperationResponse;
import com.rll.microservices.common.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Example;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RefreshScope
@RestController
public class BooksController {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private AuthorsClient authorsClient;
    
    @Autowired
    private BookDAO bookDAO;

    private BookDTO modelToDTO(Book book, AuthorDTO author) {
        BookDTO bookDTO = new BookDTO();
        bookDTO.setBook_id(book.id);
        bookDTO.setBook_isbn(book.isbn);
        bookDTO.setBook_title(book.title);
        bookDTO.setBook_description(book.description);
        bookDTO.setAuthor_id(author.getAuthor_id());
        bookDTO.setAuthor(author);

        return bookDTO;
    }

    private Book DTOToModel(BookDTO bookDTO) {
        Book book = new Book();
        book.isbn = bookDTO.getBook_isbn();
        book.title = bookDTO.getBook_title();
        book.description = bookDTO.getBook_description();
        book.author = bookDTO.getAuthor_id();

        return book;
    }

    @RequestMapping(path = "/books/init", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    void initBooks() {

        Book book1 = new Book("ISO-331", "This is a test book", "The test book was written for test purposes", "1");
        Book book2 = new Book("ISO-332", "This is the second test book", "The second test book was written for test purposes", "1");
        Book book3 = new Book("ISO-333", "This is the third test book", "The third test book was written for test purposes", "2");

        bookDAO.save(Arrays.asList(book1, book2, book3));
    }

    @RequestMapping(value = "/books", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    GetBooksResponse getBooks(
            @RequestParam(value = "book_title", required = false) String bookTitle,
            @RequestParam(value = "book_isbn", required = false) String bookIsbn) {

        GetBooksResponse response = new GetBooksResponse();
        List<BookDTO> bookDTOs = new ArrayList<BookDTO>();

        Book bookProbe = new Book();
        bookProbe.title = bookTitle;
        bookProbe.isbn = bookIsbn;

        Example<Book> bookQuery = Example.of(bookProbe);

        List<Book> books = bookDAO.findAll(bookQuery);

        for (Book book : books)
        {
            GetAuthorResponse authorResponse = authorsClient.getAuthorData(book);

            bookDTOs.add(this.modelToDTO(book, authorResponse.getResult()));
        }

        if (!bookDTOs.isEmpty())
        {
            response.setResult(bookDTOs);
        }
        else
        {
            CommonUtils.generateError(response, "BOOKS_GETLIST_001", "Unable to retrieve books");
        }

        return response;
    }

    @RequestMapping(path = "/books", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    OperationResponse createBook(@RequestBody BookDTO book) {
        OperationResponse response = new OperationResponse();

        try
        {
            Book bookEntity = this.DTOToModel(book);
            bookDAO.insert(bookEntity);

            CommonUtils.generateSuccess(response, "Book successfully persisted");
        }
        catch (Exception ex)
        {
            CommonUtils.generateError(response, "BOOKS_INSERT_001", "Unable to persist book: " + ex.getMessage());
        }

        return response;
    }

    @RequestMapping(path = "/books", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    OperationResponse updateBook(@RequestBody BookDTO book) {
        OperationResponse response = new OperationResponse();

        try
        {
            Book bookEntity = bookDAO.findOne(book.getBook_id());
            bookEntity.title = book.getBook_title();
            bookEntity.description = book.getBook_description();

            bookDAO.save(bookEntity);

            CommonUtils.generateSuccess(response, "Book successfully updated");
        }
        catch (Exception ex)
        {
            CommonUtils.generateError(response, "BOOKS_UPDATE_001", "Unable to update book: " + ex.getMessage());
        }

        return response;
    }

    @RequestMapping(path = "/books/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    GetBookResponse getBook(@PathVariable("id") String id) {

        GetBookResponse response = new GetBookResponse();

        Book book = bookDAO.findOne(id);

        if (book != null)
        {
            GetAuthorResponse authorResponse = authorsClient.getAuthorData(book);

            BookDTO bookDTO = this.modelToDTO(book, authorResponse.getResult());
            response.setResult(bookDTO);
        }
        else
        {
            CommonUtils.generateError(response, "BOOKS_GETSINGLE_001", "Unable to retrieve book");
        }

        return response;
    }

    @RequestMapping(path = "/books/{id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    OperationResponse deleteBook(@PathVariable("id") String id) {
        OperationResponse response = new OperationResponse();

        try
        {
            bookDAO.delete(id);

            CommonUtils.generateSuccess(response, "Book successfully deleted");
        }
        catch (Exception ex)
        {
            CommonUtils.generateError(response, "BOOKS_DELETE_001", "Unable to delete book: " + ex.getMessage());
        }

        return response;
    }

    @RequestMapping(path = "/books/isbn/{isbn}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    GetBookResponse getBookByISBN(@PathVariable("isbn") String isbn) {

        GetBookResponse response = new GetBookResponse();

        Book book = bookDAO.findByIsbn(isbn);

        if (book != null)
        {
            GetAuthorResponse authorResponse = authorsClient.getAuthorData(book);

            BookDTO bookDTO = this.modelToDTO(book, authorResponse.getResult());
            response.setResult(bookDTO);
        }
        else
        {
            CommonUtils.generateError(response, "BOOKS_GETSINGLE_002", "Unable to retrieve book by ISBN");
        }

        return response;
    }
}
