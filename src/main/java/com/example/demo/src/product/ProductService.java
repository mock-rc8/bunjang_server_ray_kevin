package com.example.demo.src.product;


import com.example.demo.config.BaseException;
import com.example.demo.src.product.model.*;
import com.example.demo.utils.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.demo.config.BaseResponseStatus.*;

/**
 * Service란?
 * Controller에 의해 호출되어 실제 비즈니스 로직과 트랜잭션을 처리: Create, Update, Delete 의 로직 처리
 * 요청한 작업을 처리하는 관정을 하나의 작업으로 묶음
 * dao를 호출하여 DB CRUD를 처리 후 Controller로 반환
 */
@Service    // [Business Layer에서 Service를 명시하기 위해서 사용] 비즈니스 로직이나 respository layer 호출하는 함수에 사용된다.
// [Business Layer]는 컨트롤러와 데이터 베이스를 연결
public class ProductService {
    final Logger logger = LoggerFactory.getLogger(this.getClass()); // Log 처리부분: Log를 기록하기 위해 필요한 함수입니다.

    // *********************** 동작에 있어 필요한 요소들을 불러옵니다. *************************
    private final ProductDao productDao;
    private final ProductProvider productProvider;
    private final JwtService jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!


    @Autowired //readme 참고
    public ProductService(ProductDao productDao, ProductProvider productProvider, JwtService jwtService) {
        this.productDao = productDao;
        this.productProvider = productProvider;
        this.jwtService = jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!

    }

    /**
     * 상품 등록
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostNewProductRes postNewProduct(int uid, PostNewProductReq newProduct) throws BaseException {
        NewProductModel newProductModel = new NewProductModel();
        List<String> checkExistOptions = new ArrayList<>();

        try {
            // name Validation
            if (newProduct.getName() == null)
                throw new BaseException(EMPTY_NAME); // EMPTY_NAME|2XXX|name을 입력해주세요.
            else if (newProduct.getName().length() > 100) // (Validation) 길이 확인
                throw new BaseException(TOO_LONG_TITLE); // TOO_LONG_TITLE| |name의 길이가 100자를 초과하였습니다.
            else
                newProductModel.setName(newProduct.getName());

            // content Validation
            if (newProduct.getContent() == null)
                throw new BaseException(EMPTY_CONTENT); // EMPTY_CONTENT|2XXX|content를 입력해주세요.
            else if (newProduct.getContent().length() > 1000) // (Validation) 길이 확인
                throw new BaseException(TOO_LONG_CONTENT); // TOO_LONG_CONTENT| |content의 길이가 1000자를 초과하였습니다.
            else
                newProductModel.setContent(newProduct.getContent());

            // imageUrls Validation
            if (newProduct.getImageUrls() == null)
                throw new BaseException(EMPTY_IMAGEURL_LIST); // EMPTY_IMAGEURL_LIST|| imageUrls 리스트를 입력해주세요
            else if (newProduct.getImageUrls().size() == 0)
                throw new BaseException(EMPTY_IMAGEURL); // EMPTY_IMAGEURL|2XXX|imageUrl을 한 개 이상 입력해주세요.
            else if (newProduct.getImageUrls().size() > 10)
                throw new BaseException(TOO_MANY_IMAGEURL); // TOO_MANY_IMAGEURL|2XXX|imageUrl의 수가 10개를 초과하였습니다.
            for (int i = 0; i < newProduct.getImageUrls().size(); i++) {
                if (newProduct.getImageUrls().get(i).length() > 500)
                    switch (i + 1) {
                        case 1:
                            throw new BaseException(TOO_LONG_IMAGEURL00); // TOO_LONG_IMAGEURL01|2XXX|imageUrl의 0 번째 항목이 500자를 초과하였습니다.
                        case 2:
                            throw new BaseException(TOO_LONG_IMAGEURL01); // TOO_LONG_IMAGEURL02|2XXX|imageUrl의 1 번째 항목이 500자를 초과하였습니다.
                        case 3:
                            throw new BaseException(TOO_LONG_IMAGEURL02); // |2XXX|imageUrl의 2 번째 항목이 500자를 초과하였습니다.
                        case 4:
                            throw new BaseException(TOO_LONG_IMAGEURL03); // |2XXX|imageUrl의 3 번째 항목이 500자를 초과하였습니다.
                        case 5:
                            throw new BaseException(TOO_LONG_IMAGEURL04); // |2XXX|imageUrl의 4 번째 항목이 500자를 초과하였습니다.
                        case 6:
                            throw new BaseException(TOO_LONG_IMAGEURL05); // |2XXX|imageUrl의 5 번째 항목이 500자를 초과하였습니다.
                        case 7:
                            throw new BaseException(TOO_LONG_IMAGEURL06); // |2XXX|imageUrl의 6 번째 항목이 500자를 초과하였습니다.
                        case 8:
                            throw new BaseException(TOO_LONG_IMAGEURL07); // |2XXX|imageUrl의 7 번째 항목이 500자를 초과하였습니다.
                        case 9:
                            throw new BaseException(TOO_LONG_IMAGEURL08); // |2XXX|imageUrl의 8 번째 항목이 500자를 초과하였습니다.
                        case 10:
                            throw new BaseException(TOO_LONG_IMAGEURL09); // |2XXX|imageUrl의 9 번째 항목이 500자를 초과하였습니다.
                    }
            }

            // category Validation
            // v d1 있는지 체크
            // v d1 존재하는지 체크
            // v d1.hasD 체크 && d2 있는지 체크 v
            // v d1 d2 연관성 체크v
            // v d2.hasD 체크 && d3 있는지 체크
            // v d2 d3 연관성 체크
            if (newProduct.getCategoryDepth1Id() == null)
                throw new BaseException(EMPTY_CATEGORYD1ID); // |2XXX|categoryDepth1Id을 입력해주세요.
            GetCategoryDepth01Res category01Info = productProvider.getCategoryInfoDepth01(newProduct.getCategoryDepth1Id());
            if (category01Info.getDepth1Id() == 0)
                throw new BaseException(INVALID_CATEGORYD1ID); // |3XXX|잘못된 categoryDepth1Id 입니다.
            else if (!category01Info.isHasMoreDepth()) { // 더 이상 세부항목이 없다면
                newProductModel.setCategoryDepth1Id(newProduct.getCategoryDepth1Id());
                newProductModel.setCategoryDepth2Id(0);
                newProductModel.setCategoryDepth3Id(0);
            } else if (newProduct.getCategoryDepth2Id() == null) // d1.hasD 있음 && d2 있는지 체크
                throw new BaseException(EMPTY_CATEGORYD2ID); // |2XXX|categoryDepth2Id을 입력해주세요.
            else { // d1.hasD 있음 && d2 있음
                if (!productProvider.isMatchCategory1and2(newProduct.getCategoryDepth1Id(), newProduct.getCategoryDepth2Id()))
                    throw new BaseException(NOT_MATCH_CATEGORY_12_ID); // NOT_MATCH_CATEGORY_ID|3330|연관되지 않은 depth1Id와 depth2Id입니다.
                GetCategoryDepth02Res category02Info = productProvider.getCategoryInfoDepth02(newProduct.getCategoryDepth2Id());
                if (category02Info.getDepth2Id() == 0)
                    throw new BaseException(INVALID_CATEGORYD2ID); // |3XXX|잘못된 categoryDept2Id 입니다.
                else if (!category02Info.isHasMoreDepth()) {// 더 이상 세부항목이 없다면
                    newProductModel.setCategoryDepth1Id(newProduct.getCategoryDepth1Id());
                    newProductModel.setCategoryDepth2Id(newProduct.getCategoryDepth2Id());
                    newProductModel.setCategoryDepth3Id(0);
                } else if (newProduct.getCategoryDepth3Id() == null)
                    throw new BaseException(EMPTY_CATEGORYD3ID); // |2XXX|categoryDepth3Id을 입력해주세요.
                else { // d2.hasD 있음 && d3 있음
                    if (!productProvider.isMatchCategory2and3(newProduct.getCategoryDepth2Id(), newProduct.getCategoryDepth3Id()))
                        throw new BaseException(NOT_MATCH_CATEGORY_23_ID); // NOT_MATCH_CATEGORY_ID|3330|연관되지 않은 depth2Id와 depth3Id입니다.
                    else {
                        newProductModel.setCategoryDepth1Id(newProduct.getCategoryDepth1Id());
                        newProductModel.setCategoryDepth2Id(newProduct.getCategoryDepth2Id());
                        newProductModel.setCategoryDepth3Id(newProduct.getCategoryDepth3Id());
                    }
                }
            }

            // tags Validation
            if (newProduct.getTags() != null) {
                if (newProduct.getTags().size() > 5)
                    throw new BaseException(TOO_MANY_TAGS); // TOO_MANY_TAGS|2XXX|tag의 수가 5개를 초과하였습니다.
                else if (newProduct.getTags().size() > 0)
                    checkExistOptions.add("tags");
                for (String tag : newProduct.getTags()) {
                    if (tag == null) {
                        continue;
                    }
                    if (tag.length() > 15)
                        throw new BaseException(TOO_LONG_TAGS); // TOO_LONG_TAGS|2XXX|tag의 글자수가 15자를 초과하였습니다.
                }
            }

            // price Validation
            if (newProduct.getPrice() == null)
                throw new BaseException(EMPTY_PRICE); // |2XXX|price를 입력해주세요
            else
                newProductModel.setPrice(newProduct.getPrice());

            // deliveryFee Validation
            if (newProduct.getDeliveryFee() == null)
                throw new BaseException(EMPTY_DELIVERYFREE); // |2XXX|deliveryFree를 입력해주세요
            else if (newProduct.getDeliveryFee())
                newProductModel.setDeliveryFee("true");
            else
                newProductModel.setDeliveryFee("false");

            // quantity Validation
            if (newProduct.getQuantity() == null)
                throw new BaseException(EMPTY_QUANTITY); // |2XXX|quantity를 입력해주세요
            else
                newProductModel.setQuantity(newProduct.getQuantity());

            if (newProduct.getCondition() == null)
                throw new BaseException(EMPTY_CONDITION); // |2XXX|condition를 입력해주세요
            else if (newProduct.getCondition().equals("새상품") || newProduct.getCondition().equals("중고상품"))
                newProductModel.setCondition(newProduct.getCondition());
            else
                throw new BaseException(INVALID_CONDITION); // |2XXX|condition은 '새상품' 혹은 '중고상품' 으로 기록해주세요


            // change Validation
            if (newProduct.getChange() == null)
                throw new BaseException(EMPTY_CHANGE); // |2XXX|change를 입력해주세요
            else if (newProduct.getChange())
                newProductModel.setChange("true");
            else
                newProductModel.setChange("false");

            // location Validation
            if (newProduct.getLocation() != null) {
                if (newProduct.getLocation().length() > 50)
                    throw new BaseException(TOO_LONG_LOCATION); // |2XXX|location의 길이가 50자를 초과하였습니다.
                checkExistOptions.add("location");
            }
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            logger.error(exception.getMessage());
            throw new BaseException(VALIDATION_ERROR);
        }

        int productId;
        // 새로운 Product 생성
        try {
            productId = productDao.insertNewProduct(uid, newProductModel);

        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            logger.error(exception.getMessage());
            logger.error(exception.toString());
            throw new BaseException(NEW_PRODUCT_ERROR);
        }
        // 태그 추가
        try {
            if (checkExistOptions.contains("tags"))
                productDao.addHashTags(productId, newProduct.getTags());
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            logger.error(exception.getMessage());
            throw new BaseException(NEW_TAGS_ERROR);
        }
        // image 추가
        try {
            productDao.addImgUrls(productId, newProduct.getImageUrls());
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            logger.error(exception.getMessage());
            throw new BaseException(NEW_IMAGES_ERROR);
        }
        // location 추가
        try {
            productDao.addLocationInfo(productId, newProduct.getLocation());
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            logger.error(exception.getMessage());
            throw new BaseException(NEW_LOCATION_ERROR);
        }

        // 결과 반환
        return new PostNewProductRes(productId, newProduct.getName());
    }

    /**
     * 상품 정보 수정
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostNewProductRes patchNewProduct(int uid, int productId, PostNewProductReq newProduct) throws BaseException {
        String updateQuery = "";

        // 1. name 존재하는지 확인
        if (newProduct.getName() != null){
            // (Validation) 길이 확인
            if (newProduct.getName().length() > 100)
                throw new BaseException(TOO_LONG_TITLE); // TOO_LONG_TITLE| |name의 길이가 100자를 초과하였습니다.
            else
                updateQuery += "  title = '"+newProduct.getName()+"',  ";
        }

        // 2. content
        if (newProduct.getContent() != null){
            if (newProduct.getContent().length() > 1000) // (Validation) 길이 확인
                throw new BaseException(TOO_LONG_CONTENT); // TOO_LONG_CONTENT| |content의 길이가 1000자를 초과하였습니다.
            else
                updateQuery += "  content = '"+newProduct.getContent()+"',  ";
        }

        // 3. price
        if (newProduct.getPrice() != null) {
            updateQuery += "  price = "+ newProduct.getPrice() +",  ";
        }

        // 4. deliveryFee
        if (newProduct.getDeliveryFee() != null) {
            if (newProduct.getDeliveryFee())
                updateQuery += " deliveryFee='true',  ";
            else
                updateQuery += " deliveryFee='false',  ";
        }

        // 5. quantity
        if (newProduct.getQuantity() != null)
            updateQuery += "  quantity = "+newProduct.getQuantity()+",  " ;

        // 6. condition
        if (newProduct.getCondition() != null) {
            if (newProduct.getCondition().equals("새상품") || newProduct.getCondition().equals("중고상품"))
                updateQuery += "  `condition` = '"+newProduct.getCondition()+"', ";
            else
                throw new BaseException(INVALID_CONDITION); // |2XXX|condition은 '새상품' 혹은 '중고상품' 으로 기록해주세요
        }

        // 7. change
        if (newProduct.getChange() != null) {
            if (newProduct.getChange())
                updateQuery += "  `change` = 'true', ";
            else
                updateQuery += "  `change` = 'false', ";
        }

        // 8. location
        if (newProduct.getLocation() != null) {
            if (newProduct.getLocation().length() > 50)
                throw new BaseException(TOO_LONG_LOCATION); // |2XXX|location의 길이가 50자를 초과하였습니다.
            updateQuery += " location = '"+newProduct.getLocation()+"', ";
        }

        // 9. category
        if (newProduct.getCategoryDepth1Id() != null) {
            GetCategoryDepth01Res category01Info = productProvider.getCategoryInfoDepth01(newProduct.getCategoryDepth1Id());
            if (category01Info.getDepth1Id() == 0)
                throw new BaseException(INVALID_CATEGORYD1ID); // |3XXX|잘못된 categoryDepth1Id 입니다.
            else if (!category01Info.isHasMoreDepth()) { // 더 이상 세부항목이 없다면
                updateQuery += "  categoryDepth1Id = " + newProduct.getCategoryDepth1Id() + ", ";
                updateQuery += "  categoryDepth2Id = NULL, ";
                updateQuery += "  categoryDepth3Id = NULL, ";
            } else if (newProduct.getCategoryDepth2Id() == null) // d1.hasD 있음 && d2 있는지 체크
                throw new BaseException(EMPTY_CATEGORYD2ID); // |2XXX|categoryDepth2Id을 입력해주세요.

            else { // d1.hasD 있음 && d2 있음
                if (!productProvider.isMatchCategory1and2(newProduct.getCategoryDepth1Id(), newProduct.getCategoryDepth2Id()))
                    throw new BaseException(NOT_MATCH_CATEGORY_12_ID); // NOT_MATCH_CATEGORY_ID|3330|연관되지 않은 depth1Id와 depth2Id입니다.
                GetCategoryDepth02Res category02Info = productProvider.getCategoryInfoDepth02(newProduct.getCategoryDepth2Id());
                if (category02Info.getDepth2Id() == 0)
                    throw new BaseException(INVALID_CATEGORYD2ID); // |3XXX|잘못된 categoryDept2Id 입니다.
                else if (!category02Info.isHasMoreDepth()) {// 더 이상 세부항목이 없다면
                    updateQuery += "  categoryDepth1Id = " + newProduct.getCategoryDepth1Id() + ", ";
                    updateQuery += "  categoryDepth2Id = " + newProduct.getCategoryDepth2Id() + ", ";
                    updateQuery += "  categoryDepth3Id = NULL, ";
                } else if (newProduct.getCategoryDepth3Id() == null)
                    throw new BaseException(EMPTY_CATEGORYD3ID); // |2XXX|categoryDepth3Id을 입력해주세요.
                else { // d2.hasD 있음 && d3 있음
                    if (!productProvider.isMatchCategory2and3(newProduct.getCategoryDepth2Id(), newProduct.getCategoryDepth3Id()))
                        throw new BaseException(NOT_MATCH_CATEGORY_23_ID); // NOT_MATCH_CATEGORY_ID|3330|연관되지 않은 depth2Id와 depth3Id입니다.
                    else {
                        updateQuery += "  categoryDepth1Id = " + newProduct.getCategoryDepth1Id() + ", ";
                        updateQuery += "  categoryDepth2Id = " + newProduct.getCategoryDepth2Id() + ", ";
                        updateQuery += "  categoryDepth3Id = " + newProduct.getCategoryDepth3Id() + ", ";
                    }
                }
            }
        }

        // 정보 수정
        try {
            productDao.updateProductInfo(productId, updateQuery);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            logger.error(exception.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }



        // 10. ImageUrls
        if (newProduct.getImageUrls() != null && newProduct.getImageUrls().size() != 0) {
            if (newProduct.getImageUrls().size() > 10)
                throw new BaseException(TOO_MANY_IMAGEURL); // TOO_MANY_IMAGEURL|2XXX|imageUrl의 수가 10개를 초과하였습니다.
            for (int i = 0; i < newProduct.getImageUrls().size(); i++) {
                if (newProduct.getImageUrls().get(i).length() > 500)
                    switch (i + 1) {
                        case 1:
                            throw new BaseException(TOO_LONG_IMAGEURL00); // TOO_LONG_IMAGEURL01|2XXX|imageUrl의 0 번째 항목이 500자를 초과하였습니다.
                        case 2:
                            throw new BaseException(TOO_LONG_IMAGEURL01); // TOO_LONG_IMAGEURL02|2XXX|imageUrl의 1 번째 항목이 500자를 초과하였습니다.
                        case 3:
                            throw new BaseException(TOO_LONG_IMAGEURL02); // |2XXX|imageUrl의 2 번째 항목이 500자를 초과하였습니다.
                        case 4:
                            throw new BaseException(TOO_LONG_IMAGEURL03); // |2XXX|imageUrl의 3 번째 항목이 500자를 초과하였습니다.
                        case 5:
                            throw new BaseException(TOO_LONG_IMAGEURL04); // |2XXX|imageUrl의 4 번째 항목이 500자를 초과하였습니다.
                        case 6:
                            throw new BaseException(TOO_LONG_IMAGEURL05); // |2XXX|imageUrl의 5 번째 항목이 500자를 초과하였습니다.
                        case 7:
                            throw new BaseException(TOO_LONG_IMAGEURL06); // |2XXX|imageUrl의 6 번째 항목이 500자를 초과하였습니다.
                        case 8:
                            throw new BaseException(TOO_LONG_IMAGEURL07); // |2XXX|imageUrl의 7 번째 항목이 500자를 초과하였습니다.
                        case 9:
                            throw new BaseException(TOO_LONG_IMAGEURL08); // |2XXX|imageUrl의 8 번째 항목이 500자를 초과하였습니다.
                        case 10:
                            throw new BaseException(TOO_LONG_IMAGEURL09); // |2XXX|imageUrl의 9 번째 항목이 500자를 초과하였습니다.
                    }
            }

            // image 추가
            try {
                List<String> impList = new ArrayList<>();
                // 기존 images 삭제
                productDao.addImgUrls(productId, Arrays.asList("","","","","","","","","",""));
                productDao.addImgUrls(productId, newProduct.getImageUrls());
            } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
                logger.error(exception.getMessage());
                throw new BaseException(NEW_IMAGES_ERROR);
            }
        }



        // tags Validation
        if (newProduct.getTags() != null) {
            if (newProduct.getTags().size() > 5)
                throw new BaseException(TOO_MANY_TAGS); // TOO_MANY_TAGS|2XXX|tag의 수가 5개를 초과하였습니다.

            for (String tag : newProduct.getTags()) {
                if (tag.length() > 15)
                    throw new BaseException(TOO_LONG_TAGS); // TOO_LONG_TAGS|2XXX|tag의 글자수가 15자를 초과하였습니다.
            }

            List<String> addTags ;
            List<String> delTags ;
            List<String> remove = new ArrayList<>();
            List<String> oldTags = productDao.getTags(productId);
            for(String t : newProduct.getTags()) {
                if (oldTags.contains(t))
                    remove.add(t);
            }
            oldTags.removeAll(remove);
            newProduct.tags.removeAll(remove);
            addTags = newProduct.getTags();
            delTags = oldTags;

            try {
                // 태그 삭제
                if (delTags.size() > 0)
                    productDao.delHashTags(productId, delTags);
                // 태그 추가
                if (addTags.size() > 0)
                    productDao.addHashTags(productId, addTags);
            } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
                logger.error(exception.getMessage());
                throw new BaseException(NEW_TAGS_ERROR);
            }
        }

        // 결과 반환
        return new PostNewProductRes(productId, newProduct.getName()); // TODO: getName db조회후 반환하기
    }

    /**
     * 상품 삭제
     */
    public PatchDeleteRes patchDeleteProduct(int uid, int productId, int kill) throws BaseException {
        if (kill == 9) {
            try {
                if (!isDeletableProductId(productId)){
                    return new PatchDeleteRes(0, "삭제 가능한 상품이 없습니다. 삭제할 상품을 먼저 deleted 해주세요!");
                }
                // 상품 지우기
                productDao.deleteProduct(productId, kill);
                return new PatchDeleteRes(productId, "(admin) 데이터가 삭제되었습니다 (admin)");
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new BaseException(DATABASE_ERROR);
            }
        } else {
            // 관련 태그 찾아 지우기
            List<String> oldTags = productDao.getTags(productId);
            if (oldTags.size() > 0)
                productDao.delHashTags(productId, oldTags);
            try {
                // 상품 지우기
                productDao.deleteProduct(productId);
                return new PatchDeleteRes(productId, "성공적으로 비활성화 되었습니다.");
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new BaseException(DATABASE_ERROR);
            }
        }
    }


    /**
     * 어드민 삭제 가능한 상품인지 검증
     */
    public boolean isDeletableProductId (int productId) {
        try {
            productDao.isDeletableProductId(productId);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("삭제 가능한 상품이 없습니다. 삭제할 상품을 먼저 deleted 해주세요!");
            return false;
        }
    }

    /**
     * 해당상품 판매자 팔로우 하기
     */
    @Transactional(rollbackFor = BaseException.class)

    public PostProductFollowRes postProductFollow(int uid, int productId) throws BaseException {
        if (productProvider.checkFollow(uid, productId) == 1) {
            throw new BaseException(FOLLOW_EXIST);
        }
        try {
            int followId = productDao.postProductFollow(uid, productId);
            String message = "팔로우 되었습니다!";
            return new PostProductFollowRes(followId, message);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 해당상품 판매자 팔로우 취소
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchProductFollowRes patchProductFollow(int uid, int productId) throws BaseException {
        // 팔로우->언팔로우->팔로우 끝난후 다시 언팔로우 하는 과정에서 문제 발생.
        // 사실 언팔로우는 계속 하여도 상관이없다.
//        if (productProvider.checkFollowFalse(uid, productId) == 1) {
//            throw new BaseException(FOLLOW_DELETE);
//        }
        try {
            productDao.patchProductFollow(uid, productId);
            String message = "언팔로우 되었습니다!";
            return new PatchProductFollowRes(message);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }


    /**
     * 해당상품 찜하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostProductBasketRes postProductBasket(int uid, int productId) throws BaseException {
        if (productProvider.checkBasket(uid, productId) == 1) {
            throw new BaseException(BASKET_EXIST);
        }
        try {
            int basketId = productDao.postProductBasket(uid, productId);
            String message = "찜목록에 추가했어요!";
            return new PostProductBasketRes(basketId, message);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 해당상품 찜하기 취소
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchProductBasketRes patchProductBasket(int uid, int productId) throws BaseException {
        // 찜하기->취소->찜하기 끝난후 다시 찜하는 과정에서 문제 발생.
        // 사실 찜하기 취소는 계속 하여도 상관이없다.
//        if (productProvider.checkBasketFalse(uid, productId) == 1) {
//            throw new BaseException(BASKET_DELETE);
//        }
        try {
            productDao.patchProductBasket(uid, productId);
            String message = "찜 해제가 완료되었어요!";
            return new PatchProductBasketRes(message);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }
    /**
     * 내 상품 상태변경
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostProductStatusRes postProductStatus(int uid, int productId, PostProductStatusReq postProductStatusReq) throws BaseException {
        try {
            productDao.postProductStatus(uid, productId, postProductStatusReq);
            String message = "상태변경이 완료되었어요!";
            return new PostProductStatusRes(message);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }

}
