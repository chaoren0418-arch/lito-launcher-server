#include "item_details.h"
#include <cassert>
#include <cstdio>
std::string encode(const std::string& s){std::string out="LITOITEM1:1234:";for(unsigned char c:s){char h[3];sprintf_s(h,"%02x",c);out+=h;}return out;}
int main(){
 LiToDetail d;std::string text="MP: +100000\nMR: -32769\n";for(int i=0;i<60;++i)text+="line\n";
 auto packet=encode(text);assert(ParseLiToDetail(packet.data(),packet.size(),d));assert(d.id==0x1234&&d.text==text&&d.lines.size()==62);
 auto keep=d;for(const std::string& bad:{"LITOITEM1:123456789:ff","LITOITEM1:1234:0","LITOITEM1:1234:00","LITOITEM1:1234:81","LITOITEM1:1234:gg"}){assert(!ParseLiToDetail(bad.data(),bad.size(),d));assert(d.text==keep.text);}
 std::string dbcs(21,'a');dbcs+="\xa4\xa4";packet=encode(dbcs);assert(ParseLiToDetail(packet.data(),packet.size(),d));assert(d.lines.size()==2&&d.lines[1].size()==2);
 packet=encode("Name\n\\fR"+std::string(23,'x')+"\n\\fYEnd");assert(ParseLiToDetail(packet.data(),packet.size(),d));assert(d.lines[1]=="\\fR"+std::string(22,'x'));assert(d.lines[2]=="\\fRx");assert(d.lines[3]=="\\fYEnd");
 packet=encode(std::string(8193,'a'));assert(!ParseLiToDetail(packet.data(),packet.size(),d));
 puts("PASS: >20 rows, wide signed values, Big5 wrapping, invalid/truncated/oversize payloads");
}
