package com.example.demo.src.feed;

import com.example.demo.config.BaseException;
import com.example.demo.src.feed.model.GetBrandRes;
import com.example.demo.src.feed.model.GetFeedRes;
import com.example.demo.src.product.model.GetCategoryDepth01Res;
import com.example.demo.src.product.model.GetCategoryDepth02Res;
import com.example.demo.src.product.model.GetCategoryDepth03Res;
import com.example.demo.utils.JwtService;
import com.example.demo.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.example.demo.config.BaseResponseStatus.*;

//Provider : Read의 비즈니스 로직 처리
@Service    // [Business Layer에서 Service를 명시하기 위해서 사용] 비즈니스 로직이나 respository layer 호출하는 함수에 사용된다.
// [Business Layer]는 컨트롤러와 데이터 베이스를 연결
/**
 * Provider란?
 * Controller에 의해 호출되어 실제 비즈니스 로직과 트랜잭션을 처리: Read의 비즈니스 로직 처리
 * 요청한 작업을 처리하는 관정을 하나의 작업으로 묶음
 * dao를 호출하여 DB CRUD를 처리 후 Controller로 반환
 */
public class FeedProvider {


    // *********************** 동작에 있어 필요한 요소들을 불러옵니다. *************************
    private final FeedDao feedDao;
    private final JwtService jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!


    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired //readme 참고
    public FeedProvider(FeedDao feedDao, JwtService jwtService) {
        this.feedDao = feedDao;
        this.jwtService = jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!
    }

    // ******************************************************************************
    private Utils utils;

    @Autowired
    public void setUtils(Utils utils) {
        this.utils = utils;
    }
    // ******************************************************************************

    /**
     * 홈화면 피드
     */
    @Transactional(rollbackFor = BaseException.class)
    public List<GetFeedRes> recommendFeedByUser(int uid, int p) throws BaseException {
        try {
            Set<Integer> productIdSet = new HashSet<>();
            Set<Integer> resultIdSet = new HashSet<>();
            Set<String> tagSet = new HashSet<>();
            // 최근 조회,찜 한 물품들 id 20개 불러오기
            productIdSet.addAll(feedDao.getPidListByViewAndBasket(uid, 1));
            // pid -> 태그 모아오기
            for (int pid : productIdSet) {
                tagSet.addAll(feedDao.getTags(pid));
            }
            // 태그 -> 관련상품 pid 모아오기
            for (String tag : tagSet) {
                resultIdSet.addAll(feedDao.getProductsByTag(tag));
            }
            // 팔로우 상점 -> 상품 pid 모아오기
            resultIdSet.addAll(feedDao.productIdsByFollowingStore(uid));
            // 이미 본 물품 제거
            resultIdSet.removeAll(productIdSet);

            // Set List로 변환 후 정렬(최신순)
            List<Integer> resultIdList = new ArrayList<>(resultIdSet);
            Collections.sort(resultIdList, Collections.reverseOrder());

            int addRows = 0, addStart = 0;
            if (p * 20 < resultIdList.size()) {
                resultIdList = resultIdList.subList(20 * (p - 1), 20 * p);
            } else if ((p - 1) * 20 < resultIdList.size()) { // page에 필요한 상품 수가 resultIdList의 수보다 적을 때
                resultIdList = resultIdList.subList(20 * (p - 1), resultIdList.size());
                addRows = 20 * (p - 1) - resultIdList.size();
            } else { // page에 필요한 상품 수가 resultIdList의 수보다 적을 때
                addStart = 20 * (p - 1) - resultIdList.size();
                addRows = 20;
            }

            List<GetFeedRes> result = new ArrayList<>();
            if (addRows != 20) { // resultIdList의 상품들 정보 조회
                String whereQuery = " AND id IN (" +
                        resultIdList.toString().substring(1, resultIdList.toString().length() - 1) + ")";
                String orderQuery = "ORDER BY createdAt DESC";
                result.addAll(feedDao.getFeed(whereQuery, orderQuery, 1));
            }
            if (addRows > 0) { // page에 필요한 상품 수가 resultIdList의 수보다 적을 때 -> 최신상품들 최신순으로 조회
                result.addAll(feedDao.getRecentFeed(addStart, addRows));
            }

            // 상품별 추가작업
            for (GetFeedRes elem : result) {
                // dibs 조회
                elem.setDibs(
                        utils.getBasketCountByProductId(
                                elem.getProductId()
                        )
                );
                // 사용자 찜 여부 조회
                elem.setUserDibed(this.isBasketByUid(uid, elem.getProductId()));
            }

            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 내피드 화면 (팔로우 상점 피드들)
     */
    @Transactional(rollbackFor = BaseException.class)
    public List<GetFeedRes> followFeedByUser(int uid, int p) throws BaseException {
        try {
            // 팔로우 상점 -> 상품 pid 모아오기
            List<Integer> resultIdList = new ArrayList<>(feedDao.productIdsByFollowingStore(uid));

            String whereQuery = " AND id IN (" +
                    resultIdList.toString().substring(1, resultIdList.toString().length() - 1) + ")";
            String orderQuery = "ORDER BY createdAt DESC";
            List<GetFeedRes> result = new ArrayList<>(feedDao.getFeed(whereQuery, orderQuery, p));

            // 상품별 추가작업
            for (GetFeedRes elem : result) {
                // dibs 조회
                elem.setDibs(
                        utils.getBasketCountByProductId(
                                elem.getProductId()
                        )
                );
                // 사용자 찜 여부 조회
                elem.setUserDibed(this.isBasketByUid(uid, elem.getProductId()));
            }
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 브랜드 리스트 조회
     */
    @Transactional(rollbackFor = BaseException.class)
    public List<GetBrandRes> getBrandList() throws BaseException {
        try {
            // 브랜드 정보 리스트 조회
            List<GetBrandRes> result = feedDao.getBrandList();
            for (GetBrandRes b : result) {
                // 각 정보 쿼리문 만들고
                String whereQuery = this.queryByBrand(b.getName());
                // 조회
                b.setProductCount(feedDao.getProductCount(whereQuery));
            }
            // count 순으로 정렬
            Collections.sort(result, Collections.reverseOrder());
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 브랜드 검색 스트링 반환
     */
    @Transactional(rollbackFor = BaseException.class)
    public String queryByBrand(String brandName) throws BaseException {
        Queue<String> brandQue;

        try {
            // Brand 이름 -> k 01 02 03 -> String 받아오기
            brandQue = new LinkedList<>(feedDao.getKeywordByBrand(brandName));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(BRAND_NOT_EXIST); // 3200|존재하지 않는 브랜드명입니다.
        }

        try {
            // NULL 확인 -> 제거
            for (int i = 0; i < 3; i++) {
                String temp = brandQue.poll();
                if (temp == null)
                    continue;
                else
                    brandQue.add(temp);
            }

            // 하나씩 검색어에 넣어서 or문 완성하기
            String whereQuery = "";
            if (brandQue.size() > 0) {
                for (int i = 0; i < brandQue.size(); i++) {
                    String keyword = brandQue.poll();
                    if (i == 0)
                        whereQuery += " AND ( title LIKE '%" + keyword
                                + "%' OR content LIKE '%" + keyword + "%' ";
                    else
                        whereQuery += " OR title LIKE '%" + keyword
                                + "%' OR content LIKE '%" + keyword + "%' ";
                }
                whereQuery += " ) ";
            }
            return whereQuery;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }


    /**
     * 상품 검색
     */
    @Transactional(rollbackFor = BaseException.class)
    public List<GetFeedRes> FeedByKeyword(int uid, String k, int p) throws BaseException {
        try {
            List<GetFeedRes> result = feedDao.FeedByKeywordOrderByDate(k, p);

            for (GetFeedRes elem : result) {
                // dibs
                elem.setDibs(
                        utils.getBasketCountByProductId(
                                elem.getProductId()
                        )
                );
                // 사용자 찜 여부
                elem.setUserDibed(this.isBasketByUid(uid, elem.getProductId()));
            }

            return result;
        } catch (BaseException e) {
            throw e;
        }
    }

    /**
     * 상품 검색 (Detail Ver.)
     */
    @Transactional(rollbackFor = BaseException.class)
    public List<GetFeedRes> getFeedRes(int uid,
                                       String q,
                                       String order,
                                       String brand,
                                       Integer c1,
                                       Integer c2,
                                       Integer c3,
                                       String onlySale,
                                       Integer min,
                                       Integer max,
                                       int p) throws BaseException {
        try {
            String orderQuery;
            String whereQuery = "";

            if (order == null || order.equals("recent"))
                orderQuery = " Order By createdAt DESC ";
            else if (order.equals("cheep"))
                orderQuery = " Order By price ";
            else if (order.equals("expensive"))
                orderQuery = " Order By price DESC ";
            else
                throw new BaseException(INVALID_FEED_ORDER); // |2xxxx| 올바르지 않은 order 입력입니다.

            // 키워드 검색
            if (q != null) {
                String[] keywords = q.split(" ");
                for (int i = 0; i < keywords.length; i++) {
                    if (i == 0)
                        whereQuery += " AND ( title LIKE '%" + keywords[i]
                                + "%' OR content LIKE '%" + keywords[i] + "%' ";
                    else
                        whereQuery += " OR title LIKE '%" + keywords[i]
                                + "%' OR content LIKE '%" + keywords[i] + "%' ";
                }
                whereQuery += ") ";
            }

            // 브랜드 검색
            if (brand != null) {
                whereQuery += this.queryByBrand(brand);
            }

            // 카테고리 검색
            if (c1 == null)
                ;
            else if (c2 == null) {
                // c1 체크
                if (getCategoryInfoDepth01(c1).getDepth1Id() == 0)
                    throw new BaseException(INVALID_CATEGORYD1ID); // |3XXX|잘못된 categoryDepth1Id 입니다.
                // validation 통과
                whereQuery += " AND categoryDepth1Id=" + c1;
            } else if (c3 == null) {
                // c1 체크
                if (getCategoryInfoDepth01(c1).getDepth1Id() == 0)
                    throw new BaseException(INVALID_CATEGORYD1ID); // |3XXX|잘못된 categoryDepth1Id 입니다.
                // c2 체크
                if (getCategoryInfoDepth02(c2).getDepth2Id() == 0)
                    throw new BaseException(EMPTY_CATEGORYD2ID); // |2XXX|categoryDepth2Id을 입력해주세요.
                // c1,c2 체크
                if (!isMatchCategory1and2(c1, c2))
                    throw new BaseException(NOT_MATCH_CATEGORY_12_ID); // NOT_MATCH_CATEGORY_ID|3330|연관되지 않은 depth1Id와 depth2Id입니다.
                // validation 통과
                whereQuery += " AND categoryDepth2Id=" + c2;
            } else {
                // c1 체크
                if (getCategoryInfoDepth01(c1).getDepth1Id() == 0)
                    throw new BaseException(INVALID_CATEGORYD1ID); // |3XXX|잘못된 categoryDepth1Id 입니다.
                // c2 체크
                if (getCategoryInfoDepth02(c2).getDepth2Id() == 0)
                    throw new BaseException(EMPTY_CATEGORYD2ID); // |2XXX|categoryDepth2Id을 입력해주세요.
                // c1,c2 체크
                if (!isMatchCategory1and2(c1, c2))
                    throw new BaseException(NOT_MATCH_CATEGORY_12_ID); // NOT_MATCH_CATEGORY_ID|3330|연관되지 않은 depth1Id와 depth2Id입니다.
                // c2,c3 체크
                if (isMatchCategory2and3(c2, c3))
                    throw new BaseException(NOT_MATCH_CATEGORY_23_ID); // NOT_MATCH_CATEGORY_ID|3330|연관되지 않은 depth2Id와 depth3Id입니다.
                // validation 통과
                whereQuery += " AND categoryDepth3Id=" + c3;
            }

            // 판매중인 상품만 검색
            if (onlySale != null) {
                if (onlySale.equals("true"))
                    whereQuery += " AND dealStatus='sale'";
                else if (onlySale.equals("false"))
                    ;
                else
                    throw new BaseException(INVALID_ONLYSALE); // |2xxx|onlySale에 true 혹은 false를 입력해주세요
            }

            if (min != null && max != null) {
                if (min > max)
                    throw new BaseException(INVALID_PRICE_RANGE); // |2xxxx|가격 범위가 올바르지 않습니다. max와 min을 확인해주세요.
                else
                    whereQuery += " AND price BETWEEN " + min + " AND " + max;
            } else if (min != null) {
                whereQuery += " AND price >= " + min;
            } else if (max != null) {
                whereQuery += " AND price <= " + max;
            }

            List<GetFeedRes> result = feedDao.getFeed(whereQuery, orderQuery, p);

            for (GetFeedRes elem : result) {
                // dibs
                elem.setDibs(
                        utils.getBasketCountByProductId(
                                elem.getProductId()
                        )
                );
                // 사용자 찜 여부
                elem.setUserDibed(this.isBasketByUid(uid, elem.getProductId()));
            }

            return result;
        } catch (BaseException e) {
            throw e;
        }
    }


    /**
     * 사용자 찜 여부 조회
     */
    @Transactional(rollbackFor = BaseException.class)
    public boolean isBasketByUid(int uid, int productId) {
        try {
            feedDao.isBasketByUid(uid, productId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    // Validation ===================================================================

    /**
     * 카테고리01 정보 확인
     */
    public GetCategoryDepth01Res getCategoryInfoDepth01(int depth1Id) {
        return feedDao.getCategoryInfoDepth01(depth1Id);
    }

    /**
     * 카테고리02 정보 확인
     */
    public GetCategoryDepth02Res getCategoryInfoDepth02(int depth1Id) {
        return feedDao.getCategoryInfoDepth02(depth1Id);
    }

    /**
     * 카테고리03 정보 확인
     */
    public GetCategoryDepth03Res getCategoryInfoDepth03(int depth3Id) {
        return feedDao.getCategoryInfoDepth03(depth3Id);
    }

    /**
     * (validation) 카테고리 아이디 d1 d2 가 일치하는지 확인
     */
    public boolean isMatchCategory1and2(int depth1Id, int depth2Id) {
        try {
            feedDao.getMatchCategory1and2(depth1Id, depth2Id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * (validation) 카테고리 아이디 d1 d2 가 일치하는지 확인
     */
    public boolean isMatchCategory2and3(int depth2Id, int depth3Id) {
        try {
            feedDao.getMatchCategory2and3(depth2Id, depth3Id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
