package com.chesscoach.app;

import android.graphics.Typeface;
import android.text.*;
import android.text.style.*;
import java.util.regex.*;

/** Native spans for coaching headings, emphasis, lists and inline chess notation. No HTML/WebView. */
final class CoachMarkdown {
    static CharSequence render(String markdown){
        SpannableStringBuilder out=new SpannableStringBuilder();
        for(String line:markdown.split("\n",-1)){
            if(out.length()>0)out.append('\n');
            Matcher heading=Pattern.compile("^#{1,3}\\s+(.+)$").matcher(line);
            boolean title=heading.matches();if(title)line=heading.group(1);
            line=line.replaceFirst("^\\s*[-*]\\s+","• ");int start=out.length();inline(out,line);
            if(title){out.setSpan(new StyleSpan(Typeface.BOLD),start,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);out.setSpan(new RelativeSizeSpan(1.1f),start,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        }
        return out;
    }
    private static void inline(SpannableStringBuilder out,String line){
        for(int p=0;p<line.length();){
            String marker=line.startsWith("**",p)?"**":line.charAt(p)=='`'?"`":line.charAt(p)=='*'?"*":null;
            if(marker!=null){int end=line.indexOf(marker,p+marker.length());if(end>p+marker.length()){
                int start=out.length();String body=line.substring(p+marker.length(),end);
                if(marker.equals("`"))out.append(body);else inline(out,body);
                Object span=marker.equals("`")?new TypefaceSpan("monospace"):new StyleSpan(marker.equals("**")?Typeface.BOLD:Typeface.ITALIC);
                out.setSpan(span,start,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);p=end+marker.length();continue;
            }}
            if(line.charAt(p)=='\\'&&p+1<line.length()&&"*`\\".indexOf(line.charAt(p+1))>=0)p++;
            out.append(line.charAt(p++));
        }
    }
}
