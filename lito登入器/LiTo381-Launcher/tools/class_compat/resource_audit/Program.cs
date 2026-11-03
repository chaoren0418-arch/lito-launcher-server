using Lin.Helper.Core.Pak;
System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);
using var pak=new PakFile(args[0]);
string wanted=args.Length>2?args[2]:"spelldesc-c.tbl";
for(int i=0;i<pak.Files.Count;i++) {
 var r=pak.Files[i];
 if(wanted=="--list" && r.FileName.Contains("effect",StringComparison.OrdinalIgnoreCase)){Console.WriteLine(r.FileName);continue;}
 if(r.FileName.Equals(wanted,StringComparison.OrdinalIgnoreCase)) {
  var b=pak.Extract(i);File.WriteAllBytes(args[1],b);Console.WriteLine($"Extracted {r.FileName}: {b.Length} bytes");return;
 }
}
if(wanted=="--list")return;
throw new InvalidOperationException("Missing resource: "+wanted);
