package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeBoardByteCountTest {
 @TempDir Path temporary;
 static final long OBJECT=0xFEDCBA98L;
 @Test void zeroRowsHasExactByteHeaderNameFeeAndEndOffset(){
  byte[] packet=Protocol.boardList(OBJECT,0x7FFFFFFFL,"LiTo Board",0x1234,List.of());
  assertArrayEquals(new byte[]{0x5F,(byte)0x98,(byte)0xBA,(byte)0xDC,(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFF,0x7F,0,0x4C,0x69,0x54,0x6F,0x20,0x42,0x6F,0x61,0x72,0x64,0,0x34,0x12},packet);
  decode(packet,OBJECT,0x7FFFFFFFL,"LiTo Board",0x1234,List.of());
 }
 @Test void eightRowsDecodeEveryActualOffsetAndMultibyteFields(){
  var rows=new ArrayList<GameStateStore.BoardTopic>();
  for(int i=0;i<8;i++)rows.add(new GameStateStore.BoardTopic(0x10203040L-i,"作者"+i,"26/10/04","主題"+i,"Body"));
  decode(Protocol.boardList(OBJECT,0x10203041L,"公告欄",65535,rows),OBJECT,0x10203041L,"公告欄",65535,rows);
  assertThrows(IllegalArgumentException.class,()->Protocol.boardList(OBJECT,0,"Board",300,Collections.nCopies(9,rows.get(0))));
  assertThrows(IllegalArgumentException.class,()->Protocol.boardReadRequest(new byte[]{0x56,1,0,0,0,0,0,0,0}));
 }
 @Test void encryptedActualListHandlerEchoesResetAndNextPageCursor()throws Exception {
  try(var f=new NativeWarehouseCompatibilityTest.Fixture(temporary)){
   NativeWarehouseCompatibilityTest.set(f.session,"phase",Enum.valueOf((Class)NativeWarehouseCompatibilityTest.get(f.session,"phase").getClass(),"BATTLE"));
   NativeWarehouseCompatibilityTest.set(f.session,"currentRoomId",1L);
   f.call("listBoardTopics",request(Protocol.BOARD_INITIAL_CURSOR));var empty=f.drain();assertEquals(1,empty.size());
   decode(empty.get(0),OBJECT,Protocol.BOARD_INITIAL_CURSOR,"LiTo Board",Protocol.BOARD_WRITE_FEE,List.of());
   for(int i=0;i<10;i++)f.state.postBoardTopic(f.session.sourceCharacterIdentity(),0,Map.of(InventoryCatalog.ADENA,100),"26/10/04","Title"+i,"Body"+i);
   String source=(String)NativeWarehouseCompatibilityTest.get(f.session,"selectedClassicServerName");
   var first=f.state.boardTopicsBefore(source,Protocol.BOARD_INITIAL_CURSOR,8);assertEquals(8,first.size());
   f.call("listBoardTopics",request(Protocol.BOARD_INITIAL_CURSOR));var encrypted=f.drain();assertEquals(1,encrypted.size());
   decode(encrypted.get(0),OBJECT,Protocol.BOARD_INITIAL_CURSOR,"LiTo Board",Protocol.BOARD_WRITE_FEE,first);
   long next=first.get(7).id();var rest=f.state.boardTopicsBefore(source,next,8);assertEquals(2,rest.size());
   f.call("listBoardTopics",request(next));var second=f.drain();assertEquals(1,second.size());decode(second.get(0),OBJECT,next,"LiTo Board",Protocol.BOARD_WRITE_FEE,rest);
   assertEquals(10,first.size()+rest.size());assertTrue(rest.stream().allMatch(row->row.id()<next));
  }
 }
 static byte[] request(long cursor){return new Protocol.Writer().c(0x55).d(OBJECT).d(cursor).bytes();}
 // Independent decoder follows retained ddC/sh/dsss, never Protocol.Reader.
 static void decode(byte[] packet,long object,long cursor,String name,int fee,List<GameStateStore.BoardTopic> rows){
  ByteBuffer b=ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);assertEquals(0x5F,Byte.toUnsignedInt(b.get()));assertEquals(object,Integer.toUnsignedLong(b.getInt()));assertEquals(cursor,Integer.toUnsignedLong(b.getInt()));assertEquals(9,b.position());assertEquals(rows.size(),Byte.toUnsignedInt(b.get()));assertEquals(10,b.position());
  assertEquals(name,nul(b));assertEquals(10+name.getBytes(Charset.forName("MS950")).length+1,b.position());assertEquals(fee,Short.toUnsignedInt(b.getShort()));
  for(var row:rows){int at=b.position();assertEquals(row.id(),Integer.toUnsignedLong(b.getInt()));assertEquals(at+4,b.position());assertEquals(row.author(),nul(b));assertEquals(row.date(),nul(b));assertEquals(row.title(),nul(b));}
  assertFalse(b.hasRemaining());
 }
 static String nul(ByteBuffer b){int start=b.position();while(b.get()!=0){}return new String(b.array(),start,b.position()-start-1,Charset.forName("MS950"));}
}
