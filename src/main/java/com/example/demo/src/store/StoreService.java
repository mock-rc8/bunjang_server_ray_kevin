package com.example.demo.src.store;


import com.example.demo.config.BaseException;
import com.example.demo.config.secret.Secret;
import com.example.demo.src.store.model.*;
import com.example.demo.utils.AES128;
import com.example.demo.utils.JwtService;
import com.example.demo.utils.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.config.BaseResponseStatus.*;

/**
 * Service란?
 * Controller에 의해 호출되어 실제 비즈니스 로직과 트랜잭션을 처리: Create, Update, Delete 의 로직 처리
 * 요청한 작업을 처리하는 관정을 하나의 작업으로 묶음
 * dao를 호출하여 DB CRUD를 처리 후 Controller로 반환
 */
@Service    // [Business Layer에서 Service를 명시하기 위해서 사용] 비즈니스 로직이나 respository layer 호출하는 함수에 사용된다.
            // [Business Layer]는 컨트롤러와 데이터 베이스를 연결

public class StoreService {
    final Logger logger = LoggerFactory.getLogger(this.getClass()); // Log 처리부분: Log를 기록하기 위해 필요한 함수입니다.

    // *********************** 동작에 있어 필요한 요소들을 불러옵니다. *************************
    private final StoreDao storeDao;
    private final StoreProvider storeProvider;
    private final JwtService jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!
    private Verifier verifier;
    @Autowired
    public void setVerifier(Verifier verifier){
        this.verifier = verifier;
    }

    @Autowired //readme 참고
    public StoreService(StoreDao storeDao, StoreProvider storeProvider, JwtService jwtService) {
        this.storeDao = storeDao;
        this.storeProvider = storeProvider;
        this.jwtService = jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!

    }
    // 회원가입 (POST)
    @Transactional(rollbackFor = BaseException.class)
    public PostStoreRes createUser(PostStoreReq postStoreReq) throws BaseException {

        if (storeProvider.checkPhone(postStoreReq.getPhone()) == 1) {
            throw new BaseException(POST_USERS_EXISTS_USER);
        }
        String pwd;
        try {
            // 암호화: postUserReq에서 제공받은 비밀번호를 보안을 위해 암호화시켜 DB에 저장합니다.
            // ex) password123 -> dfhsjfkjdsnj4@!$!@chdsnjfwkenjfnsjfnjsd.fdsfaifsadjfjaf
            pwd = new AES128(Secret.USER_INFO_PASSWORD_KEY).encrypt(postStoreReq.getPassword()); // 암호화코드
            postStoreReq.setPassword(pwd);
        } catch (Exception ignored) { // 암호화가 실패하였을 경우 에러 발생
            throw new BaseException(PASSWORD_ENCRYPTION_ERROR);
        }
        try {
            int userIdx = storeDao.createUser(postStoreReq);
            return new PostStoreRes(userIdx);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }

    }

    // 상점 정보 수정(Patch)
    @Transactional(rollbackFor = BaseException.class)
    public void modifyStore(int storeId, PatchStoreDetailReq patchStoreDetailReq) throws BaseException {
        if (patchStoreDetailReq.getStoreName() == null) {
            throw new BaseException(EMPTY_STORENAME);
        }
        if (patchStoreDetailReq.getStoreName().length() > 10) {
            throw new BaseException(TOO_LONG_STORENAME);
        }if (patchStoreDetailReq.getIntroduce().length() > 1000) {
            throw new BaseException(TOO_LONG_INTRODUCE);
        }if (patchStoreDetailReq.getPolicy().length() > 1000) {
            throw new BaseException(TOO_LONG_POLICY);
        }if (patchStoreDetailReq.getPrecautions().length() > 1000) {
            throw new BaseException(TOO_LONG_PRECAUTIONS);
        }
        try {
            int result = storeDao.modifyStore(storeId, patchStoreDetailReq); // 해당 과정이 무사히 수행되면 True(1), 그렇지 않으면 False(0)입니다.
            if (result == 0) { // result값이 0이면 과정이 실패한 것이므로 에러 메서지를 보냅니다.
                throw new BaseException(MODIFY_FAIL_STOREINFO);
            }
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }
}
