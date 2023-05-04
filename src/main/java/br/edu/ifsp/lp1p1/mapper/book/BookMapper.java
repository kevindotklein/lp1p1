package br.edu.ifsp.lp1p1.mapper.book;

import br.edu.ifsp.lp1p1.dto.book.BookResponseDTO;
import br.edu.ifsp.lp1p1.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public abstract class BookMapper {

    public static final BookMapper INSTANCE = Mappers.getMapper(BookMapper.class);

    public abstract Book toBook(BookResponseDTO bookResponseDTODTO);

}