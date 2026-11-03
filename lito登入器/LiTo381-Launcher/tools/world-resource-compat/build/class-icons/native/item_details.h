#pragma once
#include <string>
#include <vector>
#include <cstdint>
#include <algorithm>
struct LiToDetail {
 uint32_t id{};
 std::string text;
 std::vector<std::string> lines;
 size_t captionLines{};
};
inline int LiToHex(char c) {return c>='0'&&c<='9'?c-'0':c>='a'&&c<='f'?c-'a'+10:-1;}
inline bool ParseLiToDetail(const char* s,size_t n,LiToDetail& out) {
 const std::string prefix="LITOITEM1:";
 if(n<prefix.size()+2 || n>16430 || std::string(s,prefix.size())!=prefix)return false;
 size_t at=prefix.size(),digits=0;uint32_t id=0;
 while(at<n && s[at]!=':') {int d=LiToHex(s[at++]);if(d<0 || ++digits>8)return false;id=(id<<4)|d;}
 if(!digits || at==n)return false;
 ++at;if((n-at)%2 || (n-at)/2>8192)return false;
 LiToDetail parsed;parsed.id=id;
 while(at<n){int hi=LiToHex(s[at++]),lo=LiToHex(s[at++]);if(hi<0||lo<0)return false;char ch=char((hi<<4)|lo);if(!ch)return false;parsed.text+=ch;}
 std::string line,activeColor;size_t visible=0;
 for(size_t i=0;i<parsed.text.size();) {
  unsigned char ch=parsed.text[i];
  if(ch=='\r'){++i;continue;}
  if(ch=='\n'){parsed.lines.push_back(line);if(!parsed.captionLines)parsed.captionLines=parsed.lines.size();line.clear();activeColor.clear();visible=0;++i;continue;}
  // Native \f0..\fi color escapes occupy no glyph cells. Preserve them across a wrap.
  if(ch=='\\' && i+2<parsed.text.size() && parsed.text[i+1]=='f' && parsed.text[i+2]>='0' && parsed.text[i+2]<'i') {activeColor=parsed.text.substr(i,3);line+=activeColor;i+=3;continue;}
  size_t width=ch>=0x81&&ch<=0xfe?2:1;
  if(i+width>parsed.text.size())return false;
  if(width==2){unsigned char trail=parsed.text[i+1];if(!((trail>=0x40&&trail<=0x7e)||(trail>=0xa1&&trail<=0xfe)))return false;}
  if(visible+width>22){parsed.lines.push_back(line);line=activeColor;visible=0;}
  line.append(parsed.text,i,width);visible+=width;i+=width;
 }
 if(!line.empty())parsed.lines.push_back(line);
 out=std::move(parsed);return true;
}
