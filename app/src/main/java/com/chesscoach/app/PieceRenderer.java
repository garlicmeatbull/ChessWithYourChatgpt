package com.chesscoach.app;
import android.graphics.*;
/** Original filled Staunton-inspired vector silhouettes; no external font/art assets. */
public final class PieceRenderer {
    private static final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static void shape(Canvas c,Path path,boolean white){p.setStyle(Paint.Style.FILL);p.setColor(white?0xFFFFFCF2:0xFF202B2A);c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(white?0xFF344A40:0xFFD7E2D8);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
    private static Path polygon(float...points){Path path=new Path();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();return path;}
    public static void draw(Canvas c,char piece,float x,float y,float tile,float alpha){if(piece=='.')return;boolean white=Character.isUpperCase(piece);c.save();c.translate(x+tile*.08f,y+tile*.05f);c.scale(tile*.84f/45f,tile*.90f/45f);c.saveLayerAlpha(0,0,45,45,(int)(255*alpha));Path path=new Path();
        switch(Character.toUpperCase(piece)){
            case 'P':path.addCircle(22.5f,10,5.5f,Path.Direction.CW);shape(c,path,white);path=new Path();path.moveTo(18,16);path.cubicTo(19,24,17,27,12,32);path.lineTo(33,32);path.cubicTo(28,27,26,24,27,16);path.close();shape(c,path,white);break;
            case 'R':shape(c,polygon(10,6,16,6,16,11,20,11,20,6,25,6,25,11,29,11,29,6,35,6,35,17,30,20,31,32,14,32,15,20,10,17),white);break;
            case 'N':path.moveTo(11,32);path.cubicTo(12,24,21,24,22,18);path.lineTo(13,22);path.lineTo(8,19);path.lineTo(15,10);path.lineTo(20,8);path.lineTo(22,3);path.lineTo(28,8);path.cubicTo(40,14,35,26,34,32);path.close();shape(c,path,white);p.setColor(white?0xFF344A40:0xFFE6EEE5);c.drawCircle(24,13,1.5f,p);break;
            case 'B':path.moveTo(22.5f,5);path.cubicTo(10,17,12,21,18,23);path.lineTo(15,32);path.lineTo(30,32);path.lineTo(27,23);path.cubicTo(33,21,35,17,22.5f,5);path.close();shape(c,path,white);p.setColor(white?0xFF344A40:0xFFD7E2D8);p.setStrokeWidth(2);c.drawLine(24,10,20,17,p);break;
            case 'Q':shape(c,polygon(10,12,18,20,22.5f,9,27,20,35,12,30,29,15,29),white);for(float[] q:new float[][]{{10,10},{22.5f,7},{35,10}}){path=new Path();path.addCircle(q[0],q[1],2.7f,Path.Direction.CW);shape(c,path,white);}shape(c,polygon(14,29,31,29,33,33,12,33),white);break;
            case 'K':shape(c,polygon(21,3,24,3,24,7,28,7,28,10,24,10,24,15,21,15,21,10,17,10,17,7,21,7),white);path=new Path();path.moveTo(22.5f,17);path.cubicTo(12,10,9,19,15,25);path.lineTo(15,32);path.lineTo(30,32);path.lineTo(30,25);path.cubicTo(36,19,33,10,22.5f,17);path.close();shape(c,path,white);break;
        }
        shape(c,polygon(12,32,33,32,35,36,10,36),white);shape(c,polygon(10,36,35,36,37,40,8,40),white);c.restore();c.restore();
    }
}
