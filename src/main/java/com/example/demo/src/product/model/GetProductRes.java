package com.example.demo.src.product.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetProductRes {
    private int productId;
    private int storeId;
    private String name;
    private String dealStatus;
    private List<String> imageUrls; // M
    private Integer price;
    private String location;
    private String uploaded;
    private String uploadedEasyText;
    private int  views;
    private int dibs;
    private int talks;
    private String condition;
    private int quantity;
    private boolean deliveryFee;
    private boolean change;
    private String content;
    private List<String> tags;
    private Integer categoryDepth1Id;
    private Integer categoryDepth2Id;
    private Integer categoryDepth3Id;
    private String categoryText;

//    상점명
//    사진
//    별점
//    팔로워수
//    팔로우 여부
//    해당상점 상품
//    거래후기
//    비슷한 상품
}
