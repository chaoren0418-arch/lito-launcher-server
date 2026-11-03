#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <cstdio>
#include <cstdlib>
#include <vector>
#include <fstream>
#include "shared_status_catalog.h"
static unsigned checks;
#define CHECK(x) do{++checks;if(!(x)){std::printf("FAIL line %d\n",__LINE__);return 1;}}while(0)
static std::vector<unsigned char> Read(const char* path) {
    std::ifstream f(path,std::ios::binary);
    return {std::istreambuf_iterator<char>(f),std::istreambuf_iterator<char>()};
}
static void Put(std::vector<unsigned char>& b,unsigned at,unsigned n) {
    for(unsigned i=0;i<4;++i)b[at+i]=static_cast<unsigned char>(n>>(8*i));
}
static void Seal(std::vector<unsigned char>& b) {
    Put(b,20,SharedStatusCatalog::Crc(b.data()+24,b.size()-24));
}
int main(int argc,char** argv) {
    SetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",nullptr);
    CHECK(argc==3);
    const auto good=Read(argv[1]),native=Read(argv[2]);
    CHECK(native.size()==1540);
    std::uint32_t icons[SharedStatusCatalog::Slots]{};
    CHECK(SharedStatusCatalog::Parse(good.data(),good.size(),native.data(),icons));
    CHECK(icons[0]==5397 && icons[13]==728 && icons[77]==3080);
    CHECK(icons[38]==5399 && icons[76]==0); // packet-box34 proven; reserved unchanged
    // A new XML icon update is consumed without changing DLL code or groups.
    auto changed=good;Put(changed,32,999);Seal(changed);
    CHECK(SharedStatusCatalog::Parse(changed.data(),changed.size(),native.data(),icons));
    CHECK(icons[0]==999 && icons[13]==728);
    auto bad=good;
    for(unsigned n=0;n<good.size();++n) {
        std::uint32_t before[SharedStatusCatalog::Slots];std::memcpy(before,icons,sizeof icons);
        CHECK(!SharedStatusCatalog::Parse(good.data(),n,native.data(),icons));
        CHECK(!std::memcmp(before,icons,sizeof icons));
    }
    // Corruption at every byte and semantic errors with recomputed CRC.
    for(unsigned at=0;at<good.size();++at) {
        bad=good;bad[at]^=1;
        CHECK(!SharedStatusCatalog::Parse(bad.data(),bad.size(),native.data(),icons));
        CHECK(icons[0]==999);
    }
    for(auto pair:{std::pair<unsigned,unsigned>{24,38},{24,76},{24,SharedStatusCatalog::Slots},
                   {28,999},{32,65536},{36,999},{40,999},{44,0}}) {
        bad=good;Put(bad,pair.first,pair.second);Seal(bad);
        CHECK(!SharedStatusCatalog::Parse(bad.data(),bad.size(),native.data(),icons));
        CHECK(icons[0]==999);
    }
    auto drift=native;drift[4]^=1;
    CHECK(!SharedStatusCatalog::Parse(good.data(),good.size(),drift.data(),icons));
    bad=good;bad.push_back(0);
    CHECK(!SharedStatusCatalog::Parse(bad.data(),bad.size(),native.data(),icons));
    CHECK(SharedStatusCatalog::Load(GetModuleHandleW(nullptr),native.data(),icons));
    CHECK(icons[0]==5397 && icons[86]==3111);
    // Load handle isolation: system DLL directory must not fall back to CWD.
    CHECK(!SharedStatusCatalog::Load(GetModuleHandleW(L"kernel32.dll"),native.data(),icons));
    CHECK(SetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",L"shared_status.bin"));
    CHECK(!SharedStatusCatalog::Load(GetModuleHandleW(nullptr),native.data(),icons));
    wchar_t catalogPath[32768]{};
    CHECK(GetFullPathNameW(L"shared_status.bin",32768,catalogPath,nullptr)>0);
    CHECK(SetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",catalogPath));
    CHECK(SharedStatusCatalog::Load(GetModuleHandleW(L"kernel32.dll"),native.data(),icons));
    CHECK(icons[0]==5397 && icons[13]==728);
    SetEnvironmentVariableW(L"LITO_SHARED_STATUS_CATALOG",nullptr);
    std::printf("PASS %u checks: transactional parsing, corruption, identities, bounded file load\n",checks);
}
