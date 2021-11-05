package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.config.IcConfig;
import com.cobaltplatform.ic.backend.model.response.PublicKeyResponse;
import com.cobaltplatform.ic.backend.service.BusinessDayUtil;
import io.javalin.http.Handler;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;

public class SystemController {
    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);
    public static Handler getPublicKey = ctx -> {
        var publicKey = IcConfig.getKeyPair().getPublic();
        PublicKeyResponse publicKeyResponse =
            new PublicKeyResponse().setPublicKey(publicKey.getEncoded()).setJcaAlgorithm(
                IcConfig.getJcaName()).setFormat(publicKey.getFormat())
                .setAlgorithm(publicKey.getAlgorithm());

        ctx.json(publicKeyResponse).status(HttpStatus.SC_OK);
    };

    public static Handler isBusinessHours = ctx -> {
        var isBusinessHours = new BusinessDayUtil().isInstantWithinBusinessHours(Instant.now(), BusinessDayUtil.Department.COBALT);

        HashMap<String, Boolean> response = new HashMap<>();
        response.put("isBusinessHours", isBusinessHours);
        ctx.json(response);
    };
}
