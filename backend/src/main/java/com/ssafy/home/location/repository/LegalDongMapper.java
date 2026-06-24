package com.ssafy.home.location.repository;

import com.ssafy.home.location.dto.response.LocationSearchResponse;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LegalDongMapper {

    @Select("""
            SELECT code,
                   sido,
                   gugun,
                   dong,
                   full_name AS fullName
            FROM legal_dongs
            WHERE active = true
              AND full_name LIKE CONCAT('%', #{keyword}, '%')
            ORDER BY
              CASE WHEN dong IS NULL THEN 1 ELSE 0 END,
              CHAR_LENGTH(full_name),
              full_name
            LIMIT #{limit}
            """)
    @ConstructorArgs({
            @Arg(column = "code", javaType = String.class),
            @Arg(column = "sido", javaType = String.class),
            @Arg(column = "gugun", javaType = String.class),
            @Arg(column = "dong", javaType = String.class),
            @Arg(column = "fullName", javaType = String.class)
    })
    List<LocationSearchResponse> searchActiveByKeyword(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );
}
