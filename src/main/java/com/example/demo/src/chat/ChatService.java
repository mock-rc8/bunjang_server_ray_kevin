package com.example.demo.src.chat;


import com.example.demo.config.BaseException;
import com.example.demo.src.chat.model.*;
import com.example.demo.config.BaseException;
import com.example.demo.src.chat.model.PostChatMessageReq;
import com.example.demo.src.chat.model.PostChatMessageRes;
import com.example.demo.utils.JwtService;
import com.example.demo.utils.Verifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.demo.config.BaseResponseStatus.*;

import static com.example.demo.config.BaseResponseStatus.DATABASE_ERROR;
import static com.example.demo.utils.ValidationRegex.isRegexAccount;
import static com.example.demo.utils.ValidationRegex.isRegexPhone;

/**
 * Service란?
 * Controller에 의해 호출되어 실제 비즈니스 로직과 트랜잭션을 처리: Create, Update, Delete 의 로직 처리
 * 요청한 작업을 처리하는 관정을 하나의 작업으로 묶음
 * dao를 호출하여 DB CRUD를 처리 후 Controller로 반환
 */
@Service    // [Business Layer에서 Service를 명시하기 위해서 사용] 비즈니스 로직이나 respository layer 호출하는 함수에 사용된다.
            // [Business Layer]는 컨트롤러와 데이터 베이스를 연결
public class ChatService {
    final Logger logger = LoggerFactory.getLogger(this.getClass()); // Log 처리부분: Log를 기록하기 위해 필요한 함수입니다.

    // *********************** 동작에 있어 필요한 요소들을 불러옵니다. *************************
    private final ChatDao chatDao;
    private final ChatProvider chatProvider;
    private final JwtService jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!
    // *********************** 동작에 있어 필요한 요소들을 불러옵니다. *************************
    private Verifier verifier;
    @Autowired
    public void setVerifier(Verifier verifier){
        this.verifier = verifier;
    }
    // *********************** 동작에 있어 필요한 요소들을 불러옵니다. *************************


    @Autowired //readme 참고
    public ChatService(ChatDao chatDao, ChatProvider chatProvider, JwtService jwtService) {
        this.chatDao = chatDao;
        this.chatProvider = chatProvider;
        this.jwtService = jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!

    }

    /**
     * 텍스트 메시지 전송
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostChatMessageRes postChatMessage(int uid, int roomId, PostChatMessageReq postChatMessageReq) throws BaseException {

        if (postChatMessageReq.getMessage().isEmpty()) {
            throw new BaseException(EMPTY_TEXT);
        }

        if (postChatMessageReq.getMessage().length() > 1000) {
            throw new BaseException(TOO_LONG_TEXT);
        }
        try {
            PostChatMessageRes postChatMessageRes = chatDao.postChatMessage(uid, roomId, postChatMessageReq);
            return new PostChatMessageRes(postChatMessageRes.getMessage(), postChatMessageRes.getCreatedAt());
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }

    }

    /**
     * 이미지 전송
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostImageRes postImageUrl(int uid, int roomId, PostImageReq postImageReq) throws BaseException {

        if (postImageReq.getImageUrl().isEmpty()) {
            throw new BaseException(EMPTY_IMAGE);
        }
        try {
            PostImageRes postImageRes = chatDao.postImageUrl(uid, roomId, postImageReq);
            return new PostImageRes(postImageRes.getImageUrl(), postImageRes.getCreatedAt());
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }

    }
    /**
     * 이모티콘 전송
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostEmoticonRes postEmoticonUrl(int uid, int roomId, PostEmoticonReq postEmoticonReq) throws BaseException {

        try {
            PostEmoticonRes postEmoticonRes = chatDao.postEmoticonUrl(uid, roomId, postEmoticonReq);
            return new PostEmoticonRes(postEmoticonRes.getEmoticonUrl(), postEmoticonRes.getCreatedAt());
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }

    }


    /**
     * 상품정보 전송
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostProductInfoRes sendProcutInfoMessage(int uid, int roomId, int productId) throws BaseException {
        try {
            // (validation) 존재하는 상품인지 확인
            if (!verifier.isPresentProductId(productId))
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.

            // 보낼 수 있는 상품인지 확인 : 내 상품 혹은 상대방의 상품인지?
            if (!chatProvider.isOurProduct(roomId, productId))
                throw new BaseException(SEND_NOT_PERMITTED); // 3401|이용자가 전송할 수 없는 상품입니다.

            // 상품 정보 가져오기
            PostProductInfoRes result;
            try {
                result = chatDao.getProductInfoMessage(productId);
            } catch (Exception exception) {
                logger.error(exception.getMessage());
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
            }

            // 상품 정보 json으로 바꿔서 DB에 저장하기
            ObjectMapper mapper = new ObjectMapper();
            String productInfoJson = mapper.writeValueAsString(result);
            chatDao.sendObjectMessage(uid, roomId, "Object_product", productInfoJson);

            // 정보 반환하기 -> getProductInfoMessage(int productId);
            return result;

        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 계좌정보 전송
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostAccountInfoRes sendAccountInfoMessage(int uid, int roomId, PostAccountInfoReq pInfo) throws BaseException {
        try {
            if (!isRegexAccount(pInfo.getAccountNum()))
                throw new BaseException(ACCOUNT_REGEX_ERR);


            // 보낼 수 있는 상품인지 확인 : 내 상품 혹은 상대방의 상품인지?
            if (!chatProvider.isMyProduct(uid, pInfo.getProductId()))
                throw new BaseException(SEND_NOT_PERMITTED); // 3401|이용자가 전송할 수 없는 상품입니다.

            // 상품 정보 가져오기
            PostAccountInfoRes result;
            try {
                result = chatDao.getAccountInfoMessage(pInfo.getProductId());
            } catch (Exception exception) {
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
            }

            // 계좌 데이터 생성하기 -> id 반환
            int lastInsertId = chatDao.newAccountInfoMessage(uid, pInfo);
            result.setAccountInfoId(lastInsertId);

            // 계좌 정보 json으로 바꿔서 DB에 저장하기
            ObjectMapper mapper = new ObjectMapper();
            String productInfoJson = mapper.writeValueAsString(result);
            chatDao.sendObjectMessage(uid, roomId, "Object_account", productInfoJson);

            // 정보 반환하기 -> getProductInfoMessage(int productId);
            return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 주소정보 전송
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostAddressInfoRes sendAddressInfoMessage(int uid, int roomId, PostAddressInfoReq pInfo) throws BaseException {
        try {
            // 입력값 리젝스
            if (!isRegexPhone(pInfo.getPhoneNum()))
                throw new BaseException(PHONE_REGEX_ERR);

            // (validation) 존재하는 상품인지 확인
            if (!verifier.isPresentProductId(pInfo.getProductId()))
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.

            // 보낼 수 있는 상품인지 확인 : 내 상품 혹은 상대방의 상품인지?
            if (!chatProvider.isYourProduct(uid, roomId, pInfo.getProductId()))
                throw new BaseException(SEND_NOT_PERMITTED); // 3401|이용자가 전송할 수 없는 상품입니다.


            // 상품 정보 가져오기
            PostAddressInfoRes result;
            try {
                result = chatDao.getAddressInfoMessage(pInfo.getProductId());
            } catch (Exception exception) {
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
            }

            // 사용자 이름 가져오기
            result.setStoreName(chatDao.getStoreNameById(uid));

            // 계좌 데이터 생성하기 -> id 반환
            int lastInsertId = chatDao.newAddressInfoMessage(uid, pInfo);
            result.setAddressInfoId(lastInsertId);

            // 계좌 정보 json으로 바꿔서 DB에 저장하기
            ObjectMapper mapper = new ObjectMapper();
            String productInfoJson = mapper.writeValueAsString(result);
            chatDao.sendObjectMessage(uid, roomId, "Object_address", productInfoJson);

            // 정보 반환하기 -> getProductInfoMessage(int productId);
            return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 직거래정보 전송
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostDealInfoRes sendDealInfoMessage(int uid, int roomId, PostDealInfoReq pInfo) throws BaseException {
        try {
            // 입력값 리젝스
            if (!isRegexPhone(pInfo.getPhoneNum()))
                throw new BaseException(PHONE_REGEX_ERR);

            // (validation) 존재하는 상품인지 확인
            if (!verifier.isPresentProductId(pInfo.getProductId()))
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.

            // 보낼 수 있는 상품인지 확인 : 내 상품 혹은 상대방의 상품인지?
            if (!chatProvider.isOurProduct(roomId, pInfo.getProductId()))
                throw new BaseException(SEND_NOT_PERMITTED); // 3401|이용자가 전송할 수 없는 상품입니다.


            // 상품 정보 가져오기
            PostDealInfoRes result;
            try {
                result = chatDao.getDealInfoMessage(pInfo.getProductId());
            } catch (Exception exception) {
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
            }

            // 계좌 데이터 생성하기 -> id 반환
            int lastInsertId = chatDao.newDealInfoMessage(uid, pInfo);
            result.setDealInfoId(lastInsertId);

            // 계좌 정보 json으로 바꿔서 DB에 저장하기
            ObjectMapper mapper = new ObjectMapper();
            String productInfoJson = mapper.writeValueAsString(result);
            chatDao.sendObjectMessage(uid, roomId, "Object_address", productInfoJson);

            // 정보 반환하기 -> getProductInfoMessage(int productId);
            return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 계좌정보 조회하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public GetAccountInfoRes viewAccountInfoMessage(int uid, int roomId, int messageId) throws BaseException {
        try {

            // 계좌정보 불러오기
            GetAccountInfoRes result;
            try {
                result = chatDao.getAccountDetail(messageId);
            } catch (IncorrectResultSizeDataAccessException error) {
                logger.error(error.getMessage());
                throw new BaseException(INVALID_MESSAGE_ID); //3403|존재하지 않는 메시지 아이디입니다.
            }

            // 상품 정보 가져오기
            PostProductInfoRes productInfo;
            try {
                productInfo = chatDao.getProductInfoMessage(result.getProductId());
            } catch (Exception exception) {
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
            }

            result.setProductName(productInfo.getName());
            result.setProductImageUrl(productInfo.getImageUrl());
            result.setPrice(productInfo.getPrice());
            result.setDeliveryFee(productInfo.isDeliveryFee());

            // status 확인 -> 취소된 거래일 경우 정보 보내지 x
            if (!result.isStatus()) {
                result.setOwner("");
                result.setBankName("");
                result.setAccountNum("");
                return result;
            } else
                return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 주소정보 조회하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public GetAddressInfoRes viewAddressInfoMessage(int uid, int roomId, int messageId) throws BaseException {
        try {

            // 주소정보 불러오기
            GetAddressInfoRes result;
            try {
                result = chatDao.getAddressDetail(messageId);
            } catch (IncorrectResultSizeDataAccessException error) {
                logger.error(error.getMessage());
                throw new BaseException(INVALID_MESSAGE_ID); //3403|존재하지 않는 메시지 아이디입니다.
            }

            // 상품 정보 가져오기
            PostProductInfoRes productInfo;
            try {
                productInfo = chatDao.getProductInfoMessage(result.getProductId());
            } catch (Exception exception) {
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
            }

            result.setProductName(productInfo.getName());
            result.setProductImageUrl(productInfo.getImageUrl());
            result.setPrice(productInfo.getPrice());
            result.setDeliveryFee(productInfo.isDeliveryFee());

            // status 확인 -> 취소된 거래일 경우 정보 보내지 x
            if (!result.isStatus()) {
                result.setAddress("");
                result.setAddressDetail("");
                result.setName("");
                result.setPhoneNum("");
                return result;
            } else
                return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 직거래정보 조회하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public GetDealInfoRes viewDealInfoMessage(int uid, int roomId, int messageId) throws BaseException {
        try {

            // 계좌정보 불러오기
            GetDealInfoRes result;
            try {
                result = chatDao.getDealDetail(messageId);
            } catch (IncorrectResultSizeDataAccessException error) {
                logger.error(error.getMessage());
                throw new BaseException(INVALID_MESSAGE_ID); //3403|존재하지 않는 메시지 아이디입니다.
            }

            // 상품 정보 가져오기
            PostProductInfoRes productInfo;
            try {
                productInfo = chatDao.getProductInfoMessage(result.getProductId());
            } catch (Exception exception) {
                throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
            }

            result.setProductName(productInfo.getName());
            result.setProductImageUrl(productInfo.getImageUrl());
            result.setPrice(productInfo.getPrice());

            // status 확인 -> 취소된 거래일 경우 정보 보내지 x
            if (!result.isStatus()) {
                result.setDate("");
                result.setLocation("");
                result.setPhoneNum("");
                return result;
            } else
                return result;


        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    /**
     * 계좌정보 거래 취소하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchCancelRes delAccountInfoMessage(int uid, int roomId, int messageId) throws BaseException {
        try {
            // (validation) 내가 취소 가능한 데이터인지 확인 - uid, mid 일치하는지 조회
            int productId = chatDao.isModifiableAccountData(uid,messageId);
            if (productId == 0 )
                throw new BaseException(MODIFY_NOT_PERMITTED); // 3403|이용자가 수정할 수 없는 메시지입니다.

            // 해당 데이터 찾아서 active -> deleted 바꿈
            chatDao.delAccountInfo(messageId);

            // 상품 정보 가져오기
            PostProductInfoRes productInfo;
            try {
                productInfo = chatDao.getProductInfoMessage(productId);
            } catch (Exception exception) {
                productInfo = new PostProductInfoRes(productId, "삭제된 상품입니다.", "", 0, false);
            }

            // cancel 데이터 만들기
            PatchCancelRes result = new PatchCancelRes("Object_account",productId, productInfo.getName(), messageId);
            // 계좌 정보 json으로 바꿔서 DB에 저장하기
            ObjectMapper mapper = new ObjectMapper();
            String productInfoJson = mapper.writeValueAsString(result);
            chatDao.sendObjectMessage(uid, roomId, "Object_cancel", productInfoJson);

            // 정보 반환하기 -> getProductInfoMessage(int productId);
            return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }
    /**
     * 배송정보 거래 취소하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchCancelRes delAddressInfoMessage(int uid, int roomId, int messageId) throws BaseException {
        try {
            // (validation) 내가 취소 가능한 데이터인지 확인 - uid, mid 일치하는지 조회
            int productId = chatDao.isModifiableAddressData(uid,messageId);
            if (productId == 0 )
                throw new BaseException(MODIFY_NOT_PERMITTED); // 3403|이용자가 수정할 수 없는 메시지입니다.

            // 해당 데이터 찾아서 active -> deleted 바꿈
            chatDao.delAddressInfo(messageId);

            // 상품 정보 가져오기
            PostProductInfoRes productInfo;
            try {
                productInfo = chatDao.getProductInfoMessage(productId);
            } catch (Exception exception) {
                productInfo = new PostProductInfoRes(productId, "삭제된 상품입니다.", "", 0, false);
            }

            // cancel 데이터 만들기
            PatchCancelRes result = new PatchCancelRes("Object_address",productId, productInfo.getName(), messageId);
            // 계좌 정보 json으로 바꿔서 DB에 저장하기
            ObjectMapper mapper = new ObjectMapper();
            String productInfoJson = mapper.writeValueAsString(result);
            chatDao.sendObjectMessage(uid, roomId, "Object_cancel", productInfoJson);

            // 정보 반환하기 -> getProductInfoMessage(int productId);
            return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }
    /**
     * 계좌정보 거래 취소하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchCancelRes delDealInfoMessage(int uid, int roomId, int messageId) throws BaseException {
        try {
            // (validation) 내가 취소 가능한 데이터인지 확인 - uid, mid 일치하는지 조회
            int productId = chatDao.isModifiableDealData(uid,messageId);
            if (productId == 0 )
                throw new BaseException(MODIFY_NOT_PERMITTED); // 3403|이용자가 수정할 수 없는 메시지입니다.

            // 해당 데이터 찾아서 active -> deleted 바꿈
            chatDao.delDealtInfo(messageId);

            // 상품 정보 가져오기
            PostProductInfoRes productInfo;
            try {
                productInfo = chatDao.getProductInfoMessage(productId);
            } catch (Exception exception) {
                productInfo = new PostProductInfoRes(productId, "삭제된 상품입니다.", "", 0, false);
            }

            // cancel 데이터 만들기
            PatchCancelRes result = new PatchCancelRes("Object_deal",productId, productInfo.getName(), messageId);
            // 계좌 정보 json으로 바꿔서 DB에 저장하기
            ObjectMapper mapper = new ObjectMapper();
            String productInfoJson = mapper.writeValueAsString(result);
            chatDao.sendObjectMessage(uid, roomId, "Object_cancel", productInfoJson);

            // 정보 반환하기 -> getProductInfoMessage(int productId);
            return result;
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }
    /**
     * 계좌정보 수정
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchObjectRes patchAccountInfo(int uid, int messageId, PatchAccountInfoReq pInfo) throws BaseException {
        try {
            // (validation) 내가 수정 가능한 데이터인지 확인 - uid, mid 일치하는지 조회
            int productId = chatDao.isModifiableAccountData(uid,messageId);
            if (productId == 0 )
                throw new BaseException(MODIFY_NOT_PERMITTED); // 3403|이용자가 수정할 수 없는 메시지입니다.

            // 기존 계좌정보 불러오기
            GetAccountInfoRes oldAccount;
            try {
                oldAccount = chatDao.getAccountDetail(messageId);
            } catch (IncorrectResultSizeDataAccessException error) {
                logger.error(error.getMessage());
                throw new BaseException(INVALID_MESSAGE_ID); //3403|존재하지 않는 메시지 아이디입니다.
            }
            // 빈칸 채우기 if null ||  ""
            if (pInfo.getOwner() == null || pInfo.getOwner().isEmpty())
                pInfo.setOwner(oldAccount.getOwner());
            if (pInfo.getBankName() == null || pInfo.getBankName().isEmpty())
                pInfo.setBankName(oldAccount.getBankName());
            if (pInfo.getAccountNum() == null || pInfo.getAccountNum().isEmpty())
                pInfo.setAccountNum(oldAccount.getAccountNum());

            // (validation) 입력값 리젝스
            if (!isRegexAccount(pInfo.getAccountNum()))
                throw new BaseException(ACCOUNT_REGEX_ERR);

            // 수정하기
            try {
                chatDao.updateAccountInfo(messageId, pInfo);
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new BaseException(DATABASE_ERROR);
            }
            return new PatchObjectRes("Object_account", messageId);

        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }
    /**
     * 배송정보 수정
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchObjectRes patchAddressInfo(int uid, int messageId, PatchAddressInfoReq pInfo) throws BaseException {
        try {
            // (validation) 내가 수정 가능한 데이터인지 확인 - uid, mid 일치하는지 조회
            int productId = chatDao.isModifiableAddressData(uid,messageId);
            if (productId == 0 )
                throw new BaseException(MODIFY_NOT_PERMITTED); // 3403|이용자가 수정할 수 없는 메시지입니다.

            // 기존 계좌정보 불러오기
            GetAddressInfoRes oldAddress;
            try {
                oldAddress = chatDao.getAddressDetail(messageId);
            } catch (IncorrectResultSizeDataAccessException error) {
                logger.error(error.getMessage());
                throw new BaseException(INVALID_MESSAGE_ID); //3403|존재하지 않는 메시지 아이디입니다.
            }
            // 빈칸 채우기 if null ||  ""
            if (pInfo.getName() == null || pInfo.getName().isEmpty())
                pInfo.setName(oldAddress.getName());
            if (pInfo.getPhoneNum() == null || pInfo.getPhoneNum().isEmpty())
                pInfo.setPhoneNum(oldAddress.getPhoneNum());
            if (pInfo.getAddress() == null || pInfo.getAddress().isEmpty())
                pInfo.setAddress(oldAddress.getAddress());
            if (pInfo.getAddressDetail() == null || pInfo.getAddressDetail().isEmpty())
                pInfo.setAddressDetail(oldAddress.getAddressDetail());

            // 입력값 리젝스
            if (!isRegexPhone(pInfo.getPhoneNum()))
                throw new BaseException(PHONE_REGEX_ERR);

            // 수정하기
            try {
                chatDao.updateAddressInfo(messageId, pInfo);
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new BaseException(DATABASE_ERROR);
            }
            return new PatchObjectRes("Object_address", messageId);

        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }
    /**
     * 계좌정보 수정
     */
    @Transactional(rollbackFor = BaseException.class)
    public PatchObjectRes patchDealInfo(int uid, int messageId, PatchDealInfoReq pInfo) throws BaseException {
        try {
            // (validation) 내가 수정 가능한 데이터인지 확인 - uid, mid 일치하는지 조회
            int productId = chatDao.isModifiableDealData(uid,messageId);
            if (productId == 0 )
                throw new BaseException(MODIFY_NOT_PERMITTED); // 3403|이용자가 수정할 수 없는 메시지입니다.

            // 기존 계좌정보 불러오기
            GetDealInfoRes oldDeal;
            try {
                oldDeal = chatDao.getDealDetail(messageId);
            } catch (IncorrectResultSizeDataAccessException error) {
                logger.error(error.getMessage());
                throw new BaseException(INVALID_MESSAGE_ID); //3403|존재하지 않는 메시지 아이디입니다.
            }
            // 빈칸 채우기 if null ||  ""
            if (pInfo.getDate() == null || pInfo.getDate().isEmpty())
                pInfo.setDate(oldDeal.getDate());
            if (pInfo.getLocation() == null || pInfo.getLocation().isEmpty())
                pInfo.setLocation(oldDeal.getLocation());
            if (pInfo.getPhoneNum() == null || pInfo.getPhoneNum().isEmpty())
                pInfo.setPhoneNum(oldDeal.getPhoneNum());

            // 입력값 리젝스
            if (!isRegexPhone(pInfo.getPhoneNum()))
                throw new BaseException(PHONE_REGEX_ERR);

            // 수정하기
            try {
                chatDao.updateDealInfo(messageId, pInfo);
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new BaseException(DATABASE_ERROR);
            }
            return new PatchObjectRes("Object_deal", messageId);

        } catch (BaseException exception) {
            throw exception;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }


    /**
     * 상품 판매자와 번개톡 시작하기
     */
    @Transactional(rollbackFor = BaseException.class)
    public PostRoomRes openNewChatRoom (int uid, int productId) throws BaseException {
        // productId -> 상대방 storeId 가져오기
        int storeId;
        try {
            storeId = chatDao.getStoreIdByProduct(productId);
        } catch (Exception exception) { // (validation) 존재하지 않는 상품
            logger.error(exception.getMessage());
            throw new BaseException(INVALID_PRODUCT_ID); // 3301|존재하지 않는 상품입니다.
        }

        // (validation) 내 상품
        if (storeId == uid)
            throw new BaseException(ITS_MY_PRODUCT); // 3405|내 상품과는 채팅이 불가능합니다.

        // 이미 생성된 채팅방인지 확인
        int roomId = chatDao.checkExistRoom(uid, storeId);
        if(roomId == 0) { // 채팅방이 없는 경우
            // 내 uid,productId,storeId -> 새톡방, roomId 가져오기
            roomId = chatDao.openNewChatRoom(productId);
            chatDao.connectChatRoom(roomId, uid, storeId);

            // 첫 메시지 보내기
            this.sendProcutInfoMessage(uid, roomId, productId);

            // roomId 반환
            return new PostRoomRes("new", roomId);
        } else { // 이미 존재하는 채팅방인 경우
            // 상품 메시지 보내기
            this.sendProcutInfoMessage(uid, roomId, productId);

            // roomId 반환
            return new PostRoomRes("exist", roomId);
        }
    }

//
//    // 회원정보 수정(Patch)
//    public void modifyUserName(PatchUserReq patchUserReq) throws BaseException {
//        try {
//            int result = chatDao.modifyUserName(patchUserReq); // 해당 과정이 무사히 수행되면 True(1), 그렇지 않으면 False(0)입니다.
//            if (result == 0) { // result값이 0이면 과정이 실패한 것이므로 에러 메서지를 보냅니다.
//                throw new BaseException(MODIFY_FAIL_USERNAME);
//            }
//        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
//            throw new BaseException(DATABASE_ERROR);
//        }
//    }
}
