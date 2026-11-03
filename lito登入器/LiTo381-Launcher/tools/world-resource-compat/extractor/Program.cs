using Lin.Helper.Core.Pak;
using Lin.Helper.Core.Xml;
using System.Security.Cryptography;
using System.Text.Json;
System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);
if(args.Length < 2 || args.Length > 3) throw new ArgumentException("archive.idx output-directory [LTlist.spr]");
string member = args.Length == 3 ? args[2] : "effectlist2.xml";
if(member != "effectlist2.xml" && member != "LTlist.spr") throw new ArgumentException("Unsupported resource");
Directory.CreateDirectory(args[1]);
using var archive = new PakFile(args[0]);
var matches = Enumerable.Range(0, archive.Files.Count)
    .Where(i => archive.Files[i].FileName.Equals(member, StringComparison.OrdinalIgnoreCase)).ToArray();
if(matches.Length != 1) throw new InvalidDataException("Expected one " + member);
var raw = archive.Extract(matches[0]);
var clear = member.EndsWith(".xml") && XmlCracker.IsEncrypted(raw) ? XmlCracker.Decrypt((byte[])raw.Clone()) : raw;
File.WriteAllBytes(Path.Combine(args[1], member), clear);
File.WriteAllText(Path.Combine(args[1], member + ".extraction.json"), JsonSerializer.Serialize(new {
    archive = Path.GetFullPath(args[0]), member,
    archiveIndexSha256 = Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(args[0]))).ToLowerInvariant(),
    encodedSha256 = Convert.ToHexString(SHA256.HashData(raw)).ToLowerInvariant(),
    decodedSha256 = Convert.ToHexString(SHA256.HashData(clear)).ToLowerInvariant(),
    bytes = clear.Length, sourceModified = false
}, new JsonSerializerOptions { WriteIndented = true }));
Console.WriteLine($"Extracted shared {member} read-only: {clear.Length} bytes");
