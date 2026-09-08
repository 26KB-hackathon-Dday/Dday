package com.dday.domain.welfare.client;

import com.dday.domain.welfare.client.dto.LcgvWelfareListItem;
import com.dday.domain.welfare.client.dto.LcgvWelfareListResponse;
import com.dday.domain.welfare.client.dto.WelfareDetailResponse;
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
 *
 * <p>CENTRAL({@link WelfareListResponse})과 LOCAL({@link LcgvWelfareListResponse})은 루트
 * 엘리먼트({@code <wantedList>})·항목 엘리먼트({@code <servList>}) 이름이 같아서 한 컨텍스트에
 * 넣으면 충돌한다. 컨텍스트를 둘로 나눈다.
 */
public final class WelfareXml {

    private static final JAXBContext CENTRAL_CONTEXT = createContext(WelfareListResponse.class, WelfareListItem.class);
    private static final JAXBContext LOCAL_CONTEXT = createContext(LcgvWelfareListResponse.class, LcgvWelfareListItem.class);
    private static final JAXBContext DETAIL_CONTEXT = createContext(WelfareDetailResponse.class);

    private WelfareXml() {
    }

    private static JAXBContext createContext(Class<?>... classes) {
        try {
            return JAXBContext.newInstance(classes);
        } catch (JAXBException e) {
            throw new IllegalStateException("복지 API JAXB 컨텍스트 생성 실패", e);
        }
    }

    public static WelfareListResponse readListResponse(String xml) {
        return read(CENTRAL_CONTEXT, xml, WelfareListResponse.class);
    }

    public static LcgvWelfareListResponse readLcgvListResponse(String xml) {
        return read(LOCAL_CONTEXT, xml, LcgvWelfareListResponse.class);
    }

    /** 상세조회 응답. 중앙·지자체 공용(루트 {@code <wantedDtl>}). */
    public static WelfareDetailResponse readDetailResponse(String xml) {
        return read(DETAIL_CONTEXT, xml, WelfareDetailResponse.class);
    }

    /** 항목 하나를 {@code <servList>} XML로 되돌린다. 원본 보존용. 실패해도 수집은 막지 않는다. */
    public static String writeItem(WelfareListItem item) {
        return write(CENTRAL_CONTEXT, item);
    }

    public static String writeLcgvItem(LcgvWelfareListItem item) {
        return write(LOCAL_CONTEXT, item);
    }

    private static <T> T read(JAXBContext context, String xml, Class<T> type) {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return type.cast(unmarshaller.unmarshal(new StringReader(xml)));
        } catch (JAXBException e) {
            throw new WelfareApiException("복지 목록 응답 파싱 실패", e);
        }
    }

    private static String write(JAXBContext context, Object item) {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(item, writer);
            return writer.toString();
        } catch (JAXBException e) {
            return null;
        }
    }
}
