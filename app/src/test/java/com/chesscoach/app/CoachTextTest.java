package com.chesscoach.app;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class CoachTextTest {
    private JSONObject response(JSONObject explanation,int tokens)throws Exception{return new JSONObject().put("model","same-model").put("explanation",explanation).put("usage",new JSONObject().put("output_tokens",tokens));}
    private JSONObject cache(JSONObject r)throws Exception{return new JSONObject().put("response",r).put("text",AnalysisJson.explanation(r));}
    @Test public void streamedSummaryNeverShowsJsonSyntaxOrIncompleteEscapes(){
        assertEquals("",CoachText.partial("{\"summary\":"));
        assertEquals("한 수",CoachText.partial("{\"summary\":\"한 수\\"));
        assertEquals("한 수",CoachText.partial("{\"summary\":\"한 수\\uAC"));
        assertEquals("한 수가\n\"좋다\"",CoachText.partial("{\"summary\":\"한 수\\uAC00\\n\\\"좋다\\\""));
    }
    @Test public void allChunkBoundariesProduceCleanPrefix()throws Exception{
        String value="나이트 `Nf3`로 **전개**하세요. \"plan\": 상대의 위협을 확인하세요.\n다음 수 😀";
        String json=new JSONObject().put("summary",value).toString();
        for(int i=0;i<=json.length();i++){String partial=CoachText.partial(json.substring(0,i));assertTrue("boundary "+i,value.startsWith(partial));}
        assertEquals(value,CoachText.partial(json));
    }
    @Test public void partialDetailsKeepHeadingsAndSkipEmptyFields(){
        String raw="{\"flow\":\"중앙을 지키세요\",\"bestMoveReason\":\"전개가 빨라";
        assertEquals("## 이번 수의 판단\n중앙을 지키세요\n\n## 추천수의 근거\n전개가 빨라",CoachText.partial(raw));
    }
    @Test public void coreAppearsBeforeItsShortExplanationAtEveryBoundary()throws Exception{
        String core="포크로 퀸을 잃어요",body="나이트의 체크와 퀸 공격을 함께 확인하세요.";
        String wire="{\"headline\":\""+core+"\",\"summary\":\""+body+"\"}";
        for(int i=0;i<=wire.length();i++){
            String preview=CoachText.partial(wire.substring(0,i));
            assertTrue(core.startsWith(CoachText.previewHeadline(preview)));
            assertTrue(body.startsWith(CoachText.previewSummary(preview)));
        }
        String reversed="{\"summary\":\""+body+"\",\"headline\":\""+core+"\"}";
        assertEquals(core,CoachText.previewHeadline(CoachText.partial(reversed)));
        assertEquals(body,CoachText.previewSummary(CoachText.partial(reversed)));
        assertEquals("a",CoachText.previewHeadline("## 핵심\na\n\nb"));
        assertEquals("b",CoachText.previewSummary("## 핵심\na\n\nb"));
    }
    @Test public void coreSurvivesDetailMergeAndLegacySummariesHaveNoInventedCore()throws Exception{
        JSONObject brief=response(new JSONObject().put("headline","수비 기물을 놓쳤어요").put("summary","상대의 공격을 먼저 확인하세요."),30);
        JSONObject detail=response(new JSONObject().put("flow","판단").put("bestMoveReason","근거").put("plan","계획"),100);
        JSONObject merged=cache(CoachText.merge(CoachText.merge(null,brief),detail));
        assertEquals("수비 기물을 놓쳤어요",CoachText.headline(merged));
        assertEquals("상대의 공격을 먼저 확인하세요.",CoachText.summary(merged));
        assertTrue(CoachText.markdown(merged).startsWith("## 핵심\n수비 기물을 놓쳤어요\n\n## 요약\n"));
        assertEquals("",CoachText.headline(cache(response(new JSONObject().put("summary","이전 요약"),20))));
    }
    @Test public void summaryAndDetailMergePreservesBothUsageRecords()throws Exception{
        JSONObject summary=response(new JSONObject().put("summary","짧은 조언"),25);
        JSONObject detail=response(new JSONObject().put("flow","판단").put("bestMoveReason","근거").put("plan","계획"),130);
        JSONObject merged=CoachText.merge(CoachText.merge(null,summary),detail);
        assertEquals("짧은 조언",CoachText.summary(cache(merged)));assertTrue(CoachText.detailed(cache(merged)));
        assertTrue(CoachText.markdown(cache(merged)).contains("## 추천수의 근거\n근거"));
        assertEquals(25,merged.getJSONObject("summaryUsage").getInt("output_tokens"));assertEquals(130,merged.getJSONObject("detailUsage").getInt("output_tokens"));
        assertFalse(CoachText.detailed(cache(summary)));
    }
    @Test public void lateSummaryDoesNotEraseDetails()throws Exception{
        JSONObject detail=response(new JSONObject().put("flow","판단").put("bestMoveReason","근거").put("plan","계획"),130);
        JSONObject merged=CoachText.merge(CoachText.merge(null,detail),response(new JSONObject().put("summary","요약"),20));
        assertTrue(CoachText.detailed(cache(merged)));assertEquals(130,merged.getJSONObject("detailUsage").getInt("output_tokens"));
    }
    @Test public void legacyCacheRemainsReadableWithoutNewRequests()throws Exception{
        JSONObject old=new JSONObject().put("text","CHATGPT / old-model\n첫 문장\n\n추천수의 근거\n근거\n\n다음 계획\n계획");
        assertEquals("첫 문장",CoachText.summary(old));assertTrue(CoachText.detailed(old));assertTrue(CoachText.markdown(old).contains("## 다음 계획"));
    }
    private String event(String type,String rest){return "data: {\"type\":\""+type+"\","+rest+"}\n\n";}
    @Test public void progressArrivesBeforeCompletedAndFinalTextIsAuthoritative()throws Exception{
        String s=event("response.output_text.delta","\"delta\":\"first\"")+event("response.output_text.delta","\"delta\":\" second\"")+event("response.completed","\"response\":{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"final text\"}]}]}");
        List<String> seen=new ArrayList<>();JSONObject result=ChatGptProtocol.completed(new StringReader(s),seen::add);
        assertEquals(Arrays.asList("first","first second"),seen);assertEquals("final text",result.getString("text"));
    }
    @Test public void interruptedStreamShowsPreviewButCannotReturnSavedResult()throws Exception{
        List<String> seen=new ArrayList<>();try{ChatGptProtocol.completed(new StringReader(event("response.output_text.delta","\"delta\":\"partial\"")),seen::add);fail();}catch(IOException expected){}
        assertEquals(Collections.singletonList("partial"),seen);
    }
    @Test public void canceledProgressAbortsInference()throws Exception{
        try{ChatGptProtocol.completed(new StringReader(event("response.output_text.delta","\"delta\":\"partial\"")),text->{throw new InterruptedIOException("cancel");});fail();}catch(InterruptedIOException expected){}
    }
    @Test public void interactiveQueuePrecedesBulkWithoutParallelRequests()throws Exception{
        CoachQueue queue=new CoachQueue();CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(3);List<String> order=Collections.synchronizedList(new ArrayList<>());
        try{
            queue.submit(()->{started.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}},false);
            assertTrue(started.await(3,TimeUnit.SECONDS));
            queue.submit(()->{order.add("bulk");done.countDown();},false);
            queue.submit(()->{order.add("old selected");done.countDown();},true);
            queue.submit(()->{order.add("latest selected");done.countDown();},true);
            release.countDown();assertTrue(done.await(3,TimeUnit.SECONDS));
            assertEquals(Arrays.asList("latest selected","old selected","bulk"),order);
        }finally{release.countDown();queue.shutdownNow();}
    }
}
