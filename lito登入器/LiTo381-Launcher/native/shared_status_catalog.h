#pragma once
#include <cstdint>
#include <cstddef>
#include <cstring>
namespace SharedStatusCatalog {
constexpr unsigned Slots = 96, Records = 95, Bytes = 24 + Records * 20;
inline std::uint32_t Word(const unsigned char* p) {
    return std::uint32_t(p[0]) | std::uint32_t(p[1]) << 8 |
           std::uint32_t(p[2]) << 16 | std::uint32_t(p[3]) << 24;
}
inline std::uint32_t Crc(const unsigned char* p, std::size_t n) {
    std::uint32_t crc = ~0u;
    for(std::size_t i=0;i<n;++i) {
        crc ^= p[i];
        for(unsigned j=0;j<8;++j) crc = (crc >> 1) ^ (0xEDB88320u & (0u-(crc&1)));
    }
    return ~crc;
}
// Does not touch output until every record and native precondition passes.
inline bool Parse(const unsigned char* data, std::size_t size,
                  const void* nativeRows, std::uint32_t (&icons)[Slots]) {
    if(!data || !nativeRows || size != Bytes || Word(data)!=0x3143534Cu ||
       Word(data+4)!=1 || Word(data+8)!=Slots || Word(data+12)!=Records ||
       Word(data+16)!=Crc(static_cast<const unsigned char*>(nativeRows),77*20) ||
       Word(data+20)!=Crc(data+24,Bytes-24)) return false;
    constexpr unsigned ids[19]={107,116,118,120,121,123,126,128,129,130,82,92,512,513,514,515,516,91,110};
    constexpr unsigned original[19]={3080,3123,3117,3097,3115,3127,3113,3119,3125,3111,0,0,0,0,0,0,0,0,0};
    std::uint32_t temporary[Slots]{};
    bool seen[Slots]{};
    const auto* base=static_cast<const unsigned char*>(nativeRows);
    for(unsigned i=0;i<77;++i)temporary[i]=Word(base+i*20);
    for(unsigned i=0;i<19;++i)temporary[77+i]=original[i];
    for(unsigned i=0;i<Records;++i) {
        const auto* row=data+24+i*20;
        unsigned slot=Word(row), effect=Word(row+4), icon=Word(row+8);
        if(slot>=Slots || slot==76 || seen[slot] || icon>65535) return false;
        unsigned expectedId=slot<77 ? slot : ids[slot-77];
        unsigned message=slot<77 ? Word(base+slot*20+12) : 0;
        if(effect!=expectedId || Word(row+12)!=temporary[slot] || Word(row+16)!=message) return false;
        seen[slot]=true;temporary[slot]=icon;
    }
    for(unsigned i=0;i<Slots;++i)if(i!=76 && !seen[i])return false;
    std::memcpy(icons,temporary,sizeof temporary);return true;
}
#ifdef _WIN32
// Startup may pass a pinned absolute path in the child environment because
// the existing launcher copies its DLL into the shared client directory.
// Otherwise use the actual module directory; never consult CWD.
inline bool Load(HMODULE module, const void* nativeRows, std::uint32_t (&icons)[Slots]) {
    wchar_t path[32768]{};
    DWORD length=GetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",path,32768);
    if(length>=32768)return false;
    if(length) {
        if(length<3 || path[1]!=L':' || (path[2]!=L'\\' && path[2]!=L'/'))return false;
    } else {
        length=GetModuleFileNameW(module,path,32768);
        if(!length || length>=32768)return false;
        wchar_t* slash=wcsrchr(path,L'\\');
        if(!slash || (slash-path)+1+18>=32768)return false;
        wcscpy_s(slash+1,32768-(slash-path)-1,L"shared_status.bin");
    }
    HANDLE file=CreateFileW(path,GENERIC_READ,FILE_SHARE_READ,nullptr,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,nullptr);
    if(file==INVALID_HANDLE_VALUE)return false;
    LARGE_INTEGER size{};unsigned char bytes[Bytes]{};DWORD read{};
    bool ok=GetFileSizeEx(file,&size) && size.QuadPart==Bytes &&
            ReadFile(file,bytes,Bytes,&read,nullptr) && read==Bytes;
    CloseHandle(file);
    return ok && Parse(bytes,Bytes,nativeRows,icons);
}
#endif
}
