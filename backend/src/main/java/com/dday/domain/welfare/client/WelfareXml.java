package com.dday.domain.welfare.client;

import com.dday.domain.welfare.client.dto.WelfareListItem;
import com.dday.domain.welfare.client.dto.WelfareListResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * 복지 API XML ↔ DTO 변환. {@link JAXBContext}는 스레드 안전해서 한 번만 만든다.
 *
 * <p>JAXB를 쓰는 이유는 build.gradle 주석 참고 — jackson-dataformat-xml은 우리 JSON 응답까지
 * 오염시킨다.
 */
public final class WelfareXml {

    private static final JAXBContext CONTEXT = createContext();

    private WelfareXml() {
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(WelfareListResponse.class, WelfareListItem.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("복지 API JAXB 컨텍스트 생성 실패", e);
        }
    }

    public static WelfareListResponse readListResponse(String xml) {
        try {
            Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
            return (WelfareListResponse) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new WelfareApiException("복지 목록 응답 파싱 실패", e);
        }
    }

    /** 항목 하나를 {@code <servList>} XML로 되돌린다. 원본 보존용. 실패해도 수집은 막지 않는다. */
    public static String writeItem(WelfareListItem item) {
        try {
            Marshaller marshaller = CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(item, writer);
            return writer.toString();
        } catch (JAXBException e) {
            return null;
        }
    }
}
