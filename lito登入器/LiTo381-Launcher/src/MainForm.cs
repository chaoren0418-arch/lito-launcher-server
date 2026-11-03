using System.Diagnostics;
using System.Drawing;
using System.Security.Cryptography;

namespace LiTo381Launcher;

public sealed class MainForm : Form
{
    private readonly TextBox _projectRoot = new() { Dock = DockStyle.Fill };
    private readonly TextBox _clientDirectory = new() { Dock = DockStyle.Fill };
    private Button _browseClient = null!;
    private readonly ComboBox _mode = new() { DropDownStyle = ComboBoxStyle.DropDownList, Dock = DockStyle.Fill };
    private readonly ComboBox _displayMode = new() { DropDownStyle = ComboBoxStyle.DropDownList, Dock = DockStyle.Fill };
    private readonly ComboBox _windowScale = new() { DropDownStyle = ComboBoxStyle.DropDownList, Dock = DockStyle.Fill };
    private readonly ComboBox _selectionFlow = new() { DropDownStyle = ComboBoxStyle.DropDownList, Dock = DockStyle.Fill };
    private readonly ComboBox _nativeService = new() { DropDownStyle = ComboBoxStyle.DropDownList, Dock = DockStyle.Fill };
    private int _selectedSourceId;
    private readonly ComboBox _serverProfile = new() { DropDownStyle = ComboBoxStyle.DropDownList, Dock = DockStyle.Fill };
    private readonly Button _loadProfile = new() { Text = "載入…", AutoSize = true };
    private readonly CheckBox _allowNetwork = new() { Text = "允許外部網路（僅限已授權伺服器）", AutoSize = true };
    private readonly CheckBox _startLocalServer = new() { Text = "啟動本機 LiTo 伺服器（唯讀資料庫）", AutoSize = true };
    private readonly CheckBox _numericBreakthrough = new()
    {
        Text = "32-bit HP／MP／AC／等級／六素質／SP／MR／四屬性抗性（LiTo Schema 4）",
        AutoSize = true
    };
    private readonly Button _validate = new() { Text = "重新檢查", AutoSize = true };
    private readonly Button _buildLab = new() { Text = "建立協定實驗版", AutoSize = true };
    private readonly Button _launch = new() { Text = "啟動 LiTo.bin", AutoSize = true };
    private readonly Button _addClient = new() { Text = "新增遊戲視窗", AutoSize = true, Enabled = false };
    private readonly System.Windows.Forms.Timer _multiClientTimer = new() { Interval = 250 };
    private bool _addingClient;
    private readonly Button _openReports = new() { Text = "開啟報告資料夾", AutoSize = true };
    private readonly Label _extendedStats = new()
    {
        Dock = DockStyle.Fill,
        AutoSize = true,
        Padding = new Padding(8),
        Text = "完整素質：尚未收到 Server 角色資料",
        ForeColor = Color.DimGray
    };
    private readonly RichTextBox _log = new()
    {
        Dock = DockStyle.Fill,
        ReadOnly = true,
        BackColor = Color.FromArgb(23, 26, 32),
        ForeColor = Color.Gainsboro,
        Font = new Font("Consolas", 10),
        BorderStyle = BorderStyle.FixedSingle
    };
    private readonly Label _status = new()
    {
        Dock = DockStyle.Fill,
        AutoEllipsis = true,
        ForeColor = Color.DimGray,
        Text = "尚未驗證"
    };

    private LauncherSettings _settings;
    private Process? _activeLauncher;
    private LocalServerRuntime? _activeServerRuntime;
    private readonly string? _isolatedConfig;
    private readonly bool _defaultMemoryMode;
    private readonly Button _cancelStartup = new() { Text = "取消／結束本次啟動", AutoSize = true, Enabled = false };
    private DefaultMemoryStartup? _memoryStartup;
    private DevelopmentStartup? _developmentStartup;
    private bool StartupOwnershipRetained => _memoryStartup?.OwnershipRetained == true || _developmentStartup?.OwnershipRetained == true;
    private CancellationTokenSource? _memoryCancellation;
    private Task? _memoryOperation;
    private bool _closeAfterMemoryCleanup;
    private readonly bool _startMemoryServerOnShown;
    private readonly bool _startMemoryGameOnShown;
    private readonly bool _closeAfterMemoryGame;
    private readonly Func<Action<string>, MemoryServerControl> _serverControlFactory;
    private readonly Action<LauncherMenuPreferences> _saveMenuPreferences;
    private readonly string? _startupContextRoot;
    private bool _gameStartInProgress;
    private readonly Button _memoryServerButton = new() { Text = "啟動記憶體測試伺服器", AutoSize = true };
    private readonly Label _memoryServerStatus = new() { AutoSize = true, ForeColor = Color.DimGray };
    private MemoryServerControl? _serverControl;
    private Task? _serverControlOperation;
    private bool _serverPreparing;
    private bool UsesPreferenceUi => _defaultMemoryMode || _isolatedConfig is not null;
    private CurrentIsolatedStartup? _isolatedStartup;
    private readonly System.Windows.Forms.Timer _isolatedTimer = new() { Interval = 250 };
    private bool _isolatedTerminal;
    private bool _isolatedPreferencesFrozen;
    private bool _loadingIsolatedPreferences;
    private LauncherMenuPreferences? _frozenIsolatedPreferences;
    private string? _isolatedConfigurationSha;
    private readonly HashSet<string> _retiredIsolatedConfigurations = new(StringComparer.Ordinal);

    public MainForm(string? isolatedConfig = null, bool defaultMemoryMode = true, bool startMemoryServer = false, bool startMemoryGame = false, bool closeAfterMemoryGame = false, bool startDevelopment = false)
        : this(isolatedConfig, defaultMemoryMode, startMemoryServer, log => new MemoryServerControl(log), startMemoryGame: startMemoryGame, closeAfterMemoryGame: closeAfterMemoryGame, startDevelopment: startDevelopment) { }

    internal MainForm(string? isolatedConfig, bool defaultMemoryMode, bool startMemoryServer,
        Func<Action<string>, MemoryServerControl> serverControlFactory,
        Action<LauncherMenuPreferences>? saveMenuPreferences = null, string? startupContextRoot = null, bool startMemoryGame = false, bool closeAfterMemoryGame = false, bool startDevelopment = false)
    {
        if ((startMemoryServer || startMemoryGame) && (isolatedConfig is not null || !defaultMemoryMode || (startMemoryServer && startMemoryGame)))
            throw new ArgumentException("One exclusive automatic startup intent requires the default GUI mode.");
        if (closeAfterMemoryGame && !startMemoryGame) throw new ArgumentException("Close-after-game requires game Start intent.");
        _isolatedConfig = isolatedConfig;
        _startMemoryServerOnShown = startMemoryServer;
        _startMemoryGameOnShown = startMemoryGame;
        _closeAfterMemoryGame = closeAfterMemoryGame;
        _serverControlFactory = serverControlFactory;
        _saveMenuPreferences = saveMenuPreferences ?? LauncherMenuPreferencesStore.Save;
        _startupContextRoot = startupContextRoot;
        _defaultMemoryMode = isolatedConfig is null && defaultMemoryMode;
        Text = "LiTo 3.81C 正式流程啟動器 0.6.71";
        MinimumSize = new Size(820, 600);
        StartPosition = FormStartPosition.CenterScreen;
        Font = new Font("Microsoft JhengHei UI", 10);
        _settings = UsesPreferenceUi ? new LauncherSettings { StartLocalServer = false } : LauncherSettingsStore.Load();
        InitializeLayout();

        if (_defaultMemoryMode) InitializeMemoryUi();
        else if (isolatedConfig is null) LoadSettingsIntoUi();
        else InitializeIsolatedUi();
        AcceptButton = _defaultMemoryMode || isolatedConfig is null ? _launch : null;
        Shown += async (_, _) =>
        {
            if (startDevelopment) await LaunchMemoryAsync();
            if (_startMemoryServerOnShown) await StartMemoryServerAsync();
            if (_startMemoryGameOnShown)
            {
                await LaunchMemoryAsync();
                if (_closeAfterMemoryGame)
                {
                    if (CanCloseAfterMemoryGame()) Close();
                    else _status.Text = "本次清理或結束紀錄尚未確認，保留啟動器以便處理。";
                }
            }
            if (UsesPreferenceUi) return; // A real Start owns the selected preferences.
            await ValidateAsync();
            if (_launch.Enabled) ActiveControl = _launch;
        };
        FormClosing += OnFormClosing;
    }

    private void InitializeLayout()
    {
        var root = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            Padding = new Padding(18),
            ColumnCount = 1,
            RowCount = 6
        };
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        root.RowStyles.Add(new RowStyle(SizeType.AutoSize));

        var title = new Label
        {
            Text = "LiTo 3.81C 專屬啟動器",
            Font = new Font(Font.FontFamily, 20, FontStyle.Bold),
            AutoSize = true,
            Margin = new Padding(0, 0, 0, 14)
        };
        root.Controls.Add(title, 0, 0);

        var settingsPanel = new TableLayoutPanel
        {
            Dock = DockStyle.Top,
            AutoSize = true,
            ColumnCount = 3,
            Padding = new Padding(0, 0, 0, 10)
        };
        settingsPanel.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 130));
        settingsPanel.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        settingsPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
        _browseClient = AddPathRow(settingsPanel, 0, "LiTo 客戶端：", _clientDirectory, () => BrowseFolder(_clientDirectory));
        settingsPanel.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        settingsPanel.Controls.Add(new Label { Text = "顯示模式：", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 1);
        settingsPanel.Controls.Add(_displayMode, 1, 1);
        settingsPanel.SetColumnSpan(_displayMode, 2);
        settingsPanel.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        settingsPanel.Controls.Add(new Label { Text = "視窗縮放：", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 2);
        settingsPanel.Controls.Add(_windowScale, 1, 2);
        settingsPanel.SetColumnSpan(_windowScale, 2);
        settingsPanel.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        settingsPanel.Controls.Add(new Label { Text = "遊戲模式：", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 3);
        settingsPanel.Controls.Add(_nativeService, 1, 3);
        settingsPanel.SetColumnSpan(_nativeService, 2);
        settingsPanel.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        settingsPanel.Controls.Add(new Label { Text = "來源伺服器：", AutoSize = true, Anchor = AnchorStyles.Left }, 0, 4);
        var sourceNotice = new Label { Text = "啟動後於原生伺服器選單選擇", AutoSize = true, Anchor = AnchorStyles.Left };
        settingsPanel.Controls.Add(sourceNotice, 1, 4);
        settingsPanel.SetColumnSpan(sourceNotice, 2);
        settingsPanel.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        settingsPanel.Controls.Add(new Label
        {
            Text = "啟動後先選伺服器，再輸入該服帳密，進入角色選單。\n"
                + "角色選單：「返回」登出至帳密頁；「伺服器」登出至選服頁。",
            AutoSize = true,
            ForeColor = Color.DimGray,
            Margin = new Padding(0, 8, 0, 4)
        }, 1, 5);
        settingsPanel.SetColumnSpan(settingsPanel.GetControlFromPosition(1, 5)!, 2);
        root.Controls.Add(settingsPanel, 0, 1);

        var actions = new FlowLayoutPanel
        {
            Dock = DockStyle.Top,
            AutoSize = true,
            WrapContents = true,
            Padding = new Padding(0, 0, 0, 10)
        };
        actions.Controls.Add(_validate);
        actions.Controls.Add(_launch);
        root.Controls.Add(actions, 0, 2);
        var extendedStatsPanel = new GroupBox
        {
            Text = "Server 完整素質（不受 LiTo.bin 原生欄寬限制）",
            Dock = DockStyle.Top,
            AutoSize = true,
            Padding = new Padding(6)
        };
        extendedStatsPanel.Controls.Add(_extendedStats);
        root.Controls.Add(extendedStatsPanel, 0, 3);
        root.Controls.Add(_log, 0, 4);
        root.Controls.Add(_status, 0, 5);
        Controls.Add(root);

        _mode.Items.AddRange(new object[]
        {
            new ModeItem(LaunchMode.ProtectedClient, "原始 LiTo.bin（建議）"),
            new ModeItem(LaunchMode.RebuiltClient, "Same-boot 重建客戶端"),
            new ModeItem(LaunchMode.ProtocolLab, "127.0.0.1 協定實驗")
        });
        _displayMode.Items.AddRange(new object[]
        {
            new DisplayModeItem(GameDisplayMode.Windowed, "視窗化（建議）")
        });
        _windowScale.Items.AddRange(new object[]
        {
            new ScaleItem(100, "100%（640×480）"),
            new ScaleItem(125, "125%（800×600）"),
            new ScaleItem(150, "150%（960×720）"),
            new ScaleItem(200, "200%（1280×960）")
        });
        _selectionFlow.Items.AddRange(new object[]
        {
            new SelectionFlowItem(SelectionFlowMode.Native, "原生帳密／頻道／角色流程（建議）"),
            new SelectionFlowItem(SelectionFlowMode.Automatic, "自動進入（僅診斷）")
        });
        _nativeService.Items.AddRange(new object[]
        {
            new NativeServiceItem(NativeServiceMode.StandardArena, "一般無界擂台（預設；無額外參數）"),
            new NativeServiceItem(NativeServiceMode.RentalServer, "官方租賃伺服器（原生 /ltpremiumsvr）")
        });
        _displayMode.SelectedIndexChanged += (_, _) => UpdateModeUi();
        _displayMode.SelectedIndexChanged += (_, _) => IsolatedPreferencesChanged();
        _windowScale.SelectedIndexChanged += (_, _) => IsolatedPreferencesChanged();
        _nativeService.SelectedIndexChanged += (_, _) => IsolatedPreferencesChanged();
        _clientDirectory.TextChanged += (_, _) => IsolatedPreferencesChanged();
        _startLocalServer.CheckedChanged += (_, _) => UpdateModeUi();
        _validate.Click += async (_, _) => await ValidateAsync();
        _launch.Click += async (_, _) => await LaunchAsync();
    }

    private static Button AddPathRow(
        TableLayoutPanel panel, int row, string label, TextBox textBox, Action browse)
    {
        panel.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        panel.Controls.Add(new Label { Text = label, AutoSize = true, Anchor = AnchorStyles.Left }, 0, row);
        panel.Controls.Add(textBox, 1, row);
        var button = new Button { Text = "瀏覽…", AutoSize = true };
        button.Click += (_, _) => browse();
        panel.Controls.Add(button, 2, row);
        return button;
    }

    private void LoadSettingsIntoUi()
    {
        _settings.ProjectRoot = WorkspaceLocator.FindProjectRoot(_settings.ProjectRoot);
        _settings.ClientDirectory = LiToClientLocator.Find(
            _settings.ClientDirectory, _settings.ProjectRoot);
        _projectRoot.Text = _settings.ProjectRoot;
        _clientDirectory.Text = _settings.ClientDirectory;
        _settings.Mode = LaunchMode.ProtectedClient;
        _settings.DisplayMode = GameDisplayMode.Windowed;
        _settings.SelectionFlow = SelectionFlowMode.Native;
        _settings.StartLocalServer = true;
        _settings.NumericBreakthroughEnabled = true;
        _settings.AllowNetwork = false;
        _settings.ServerProfilePath = ServerProfileDefinition.FindDedicatedPath();
        _settings.OriginSourceSelectionInitialized = true;
        _allowNetwork.Checked = false;
        _startLocalServer.Checked = true;
        _numericBreakthrough.Checked = true;
        ReloadServerProfiles(_settings.ServerProfilePath);
        for (var index = 0; index < _mode.Items.Count; index++)
        {
            if (((ModeItem)_mode.Items[index]!).Mode == _settings.Mode)
            {
                _mode.SelectedIndex = index;
                break;
            }
        }
        if (_mode.SelectedIndex < 0)
        {
            _mode.SelectedIndex = 0;
        }
        for (var index = 0; index < _displayMode.Items.Count; index++)
        {
            if (((DisplayModeItem)_displayMode.Items[index]!).Mode == _settings.DisplayMode)
            {
                _displayMode.SelectedIndex = index;
                break;
            }
        }
        if (_displayMode.SelectedIndex < 0)
        {
            _displayMode.SelectedIndex = 0;
        }
        for (var index = 0; index < _windowScale.Items.Count; index++)
        {
            if (((ScaleItem)_windowScale.Items[index]!).Percent == _settings.WindowScalePercent)
            {
                _windowScale.SelectedIndex = index;
                break;
            }
        }
        if (_windowScale.SelectedIndex < 0) _windowScale.SelectedIndex = 0;
        for (var index = 0; index < _selectionFlow.Items.Count; index++)
        {
            if (((SelectionFlowItem)_selectionFlow.Items[index]!).Mode == _settings.SelectionFlow)
            {
                _selectionFlow.SelectedIndex = index;
                break;
            }
        }
        if (_selectionFlow.SelectedIndex < 0) _selectionFlow.SelectedIndex = 0;
        for (var index = 0; index < _nativeService.Items.Count; index++)
        {
            if (((NativeServiceItem)_nativeService.Items[index]!).Mode == _settings.NativeService)
            {
                _nativeService.SelectedIndex = index;
                break;
            }
        }
        if (_nativeService.SelectedIndex < 0) _nativeService.SelectedIndex = 0;
    }

    private LauncherSettings ReadSettingsFromUi()
    {
        _settings.ProjectRoot = _projectRoot.Text.Trim();
        _settings.ClientDirectory = _clientDirectory.Text.Trim();
        // This project is intentionally a LiTo.bin-only launcher.  The enum is
        // retained for compatibility with older settings files and smoke tests.
        _settings.Mode = LaunchMode.ProtectedClient;
        _settings.DisplayMode = (_displayMode.SelectedItem as DisplayModeItem)?.Mode
            ?? GameDisplayMode.Windowed;
        _settings.WindowScalePercent = (_windowScale.SelectedItem as ScaleItem)?.Percent ?? 100;
        _settings.SelectionFlow = SelectionFlowMode.Native;
        _settings.NativeService = (_nativeService.SelectedItem as NativeServiceItem)?.Mode
            ?? NativeServiceMode.StandardArena;
        _settings.StartLocalServer = true;
        _settings.NumericBreakthroughEnabled = true;
        _settings.AllowNetwork = false;
        _settings.ServerProfilePath = ServerProfileDefinition.FindDedicatedPath();
        LauncherSettingsStore.Save(_settings);
        return _settings;
    }

    private void UpdateModeUi()
    {
        if (UsesPreferenceUi) { SetIsolatedPreferencesFrozen(_isolatedPreferencesFrozen); return; }
        var mode = (_mode.SelectedItem as ModeItem)?.Mode ?? LaunchMode.ProtectedClient;
        _allowNetwork.Enabled = mode != LaunchMode.ProtocolLab;
        if (!_allowNetwork.Enabled)
        {
            _allowNetwork.Checked = false;
        }
        _buildLab.Visible = mode == LaunchMode.ProtocolLab;
        _displayMode.Enabled = mode == LaunchMode.ProtectedClient;
        _windowScale.Enabled = mode == LaunchMode.ProtectedClient
            && ((_displayMode.SelectedItem as DisplayModeItem)?.Mode == GameDisplayMode.Windowed);
        _selectionFlow.Enabled = mode == LaunchMode.ProtectedClient;
        _nativeService.Enabled = mode == LaunchMode.ProtectedClient;
        _startLocalServer.Enabled = mode == LaunchMode.ProtectedClient;
        _numericBreakthrough.Enabled = mode == LaunchMode.ProtectedClient
            && _startLocalServer.Checked
            && ((_displayMode.SelectedItem as DisplayModeItem)?.Mode == GameDisplayMode.Windowed);
        _serverProfile.Enabled = mode == LaunchMode.ProtectedClient;
        _loadProfile.Enabled = mode == LaunchMode.ProtectedClient;
    }

    private async Task ValidateAsync()
    {
        if (_defaultMemoryMode)
        {
            RefreshGameAvailability();
            return;
        }
        if (_isolatedConfig is not null) { PrepareIsolated(); return; }
        try
        {
            SetBusy(true, "正在驗證檔案…");
            var result = await LauncherService.ValidateAsync(ReadSettingsFromUi());
            AppendLog($"\n[{DateTime.Now:HH:mm:ss}] 驗證模式：{result.Mode}");
            foreach (var item in result.Items)
            {
                AppendLog($"{(item.Passed ? "[OK]" : "[X]")} {item.Name}: {item.Detail}");
            }
            _status.Text = result.CanLaunch ? "驗證通過，可以啟動。" : "驗證未通過，請查看日誌。";
            _status.ForeColor = result.CanLaunch ? Color.ForestGreen : Color.Firebrick;
            _launch.Enabled = result.CanLaunch && _activeLauncher is null;
        }
        catch (Exception ex)
        {
            AppendLog("驗證失敗：" + ex);
            _status.Text = "驗證發生錯誤。";
            _status.ForeColor = Color.Firebrick;
        }
        finally
        {
            SetBusy(false);
        }
    }

    private async Task BuildProtocolLabAsync()
    {
        try
        {
            SetBusy(true, "正在建立協定實驗客戶端…");
            await RunProcessAsync(LauncherService.CreateProtocolBuildStartInfo(ReadSettingsFromUi()));
            await ValidateAsync();
        }
        catch (Exception ex)
        {
            AppendLog("建立失敗：" + ex);
            MessageBox.Show(this, ex.Message, "建立失敗", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
        finally
        {
            SetBusy(false);
        }
    }

    private async Task LaunchAsync()
    {
        if (_defaultMemoryMode) { await LaunchMemoryAsync(); return; }
        if (_isolatedConfig is not null) { StartIsolated(); return; }
        var settings = ReadSettingsFromUi();
        var validation = await LauncherService.ValidateAsync(settings);
        if (!validation.CanLaunch)
        {
            MessageBox.Show(this, "啟動前驗證未通過。", "無法啟動", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            await ValidateAsync();
            return;
        }

        if (settings.AllowNetwork)
        {
            var answer = MessageBox.Show(
                this,
                "外部網路模式只應連線到你獲准使用的伺服器。是否繼續？",
                "確認網路模式",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning,
                MessageBoxDefaultButton.Button2);
            if (answer != DialogResult.Yes)
            {
                return;
            }
        }

        var startedLocalServer = false;
        ClientResourceSnapshot? resourceSnapshot = null;
        try
        {
            if (settings.Mode == LaunchMode.ProtectedClient)
            {
                resourceSnapshot = await ClientResourceAudit.CaptureAsync(settings.ClientDirectory);
                if (resourceSnapshot.Errors.Count > 0)
                {
                    throw new InvalidOperationException(
                        "無法建立 TW/LITO 共享資源啟動前快照："
                        + string.Join("；", resourceSnapshot.Errors));
                }
                AppendLog(
                    $"[Guard] 選定客戶端共享資源啟動前指紋：{resourceSnapshot.Fingerprint[..16]}；"
                    + "LiTo 使用隔離相容執行副本，此目錄只監測、不替換");
            }
            if (settings.StartLocalServer)
            {
                startedLocalServer = await EnsureLocalServerAsync(settings);
            }
            SetBusy(true, "遊戲執行中；請先關閉遊戲，再關閉啟動器。", keepLaunchDisabled: true);
            AppendLog($"\n[{DateTime.Now:HH:mm:ss}] 啟動 {settings.Mode}");
            var startInfo = LauncherService.CreateLaunchStartInfo(settings);
            await RunProcessAsync(startInfo, trackAsLauncher: true);
            AppendLog($"[{DateTime.Now:HH:mm:ss}] 啟動流程結束");
        }
        catch (Exception ex)
        {
            AppendLog("啟動失敗：" + ex);
            MessageBox.Show(this, ex.Message, "啟動失敗", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
        finally
        {
            _activeLauncher = null;
            if (startedLocalServer) StopLocalServer();
            if (resourceSnapshot is not null)
            {
                try
                {
                    var after = await ClientResourceAudit.CaptureAsync(settings.ClientDirectory);
                    var changes = ClientResourceAudit.Compare(resourceSnapshot, after);
                    if (changes.Count == 0)
                    {
                        AppendLog(
                            $"[OK] TW/LITO 共享資源未被本次啟動修改；結束指紋 {after.Fingerprint[..16]}");
                    }
                    else
                    {
                        AppendLog("[X] 偵測到共享資源在 LITO 執行期間改變：");
                        foreach (var change in changes)
                        {
                            AppendLog($"    {change.RelativePath}: {change.Detail}");
                        }
                        MessageBox.Show(
                            this,
                            "偵測到 TW/LITO 共用資源在本次 LITO 執行期間發生變化。請查看日誌，且不要啟動 TW，直到人工確認。",
                            "共享資源保護警告",
                            MessageBoxButtons.OK,
                            MessageBoxIcon.Error);
                    }
                }
                catch (Exception ex)
                {
                    AppendLog("[Guard ERR] 啟動後共享資源比對失敗：" + ex.Message);
                }
            }
            SetBusy(false);
            await ValidateAsync();
        }
    }

    private async Task<bool> EnsureLocalServerAsync(LauncherSettings settings)
    {
        if (string.IsNullOrWhiteSpace(settings.ServerProfilePath))
        {
            throw new InvalidOperationException("請先選擇原始角色來源。");
        }
        var sourceProfile = ServerProfileDefinition.Load(settings.ServerProfilePath);
        if (!sourceProfile.IsLoopback)
        {
            throw new InvalidOperationException("本機伺服器只能搭配 loopback 原始角色來源。");
        }
        var serverRoot = LauncherService.GetServerRoot(settings.ProjectRoot);
        AppendLog($"[Server] 原始角色來源：{sourceProfile.OriginSourceName} "
            + $"({sourceProfile.OriginSourceId}); mode={settings.NativeService}");
        _activeServerRuntime = await LocalServerService.StartAsync(
            serverRoot, sourceProfile, settings.NativeService, AppendLog,
            extendedStats: UpdateExtendedStats,
            numericBreakthrough: settings.NumericBreakthroughEnabled);
        return true;
    }

    private void UpdateExtendedStats(ExtendedCharacterStatsSnapshot snapshot)
    {
        if (InvokeRequired)
        {
            BeginInvoke(() => UpdateExtendedStats(snapshot));
            return;
        }
        var database = snapshot.Database;
        var directory = snapshot.DirectoryShadow;
        var heading = $"{snapshot.Character}  [{snapshot.Source} / {snapshot.Event}]  "
            + $"Numeric Schema {snapshot.NumericBreakthroughSchema}";
        var databaseLine = $"資料庫基礎值：HP {database.MaxHp:N0}  MP {database.MaxMp:N0}  AC {database.ArmorClass:N0}  "
            + $"Lv {database.Level:N0}  STR {database.Strength:N0}  DEX {database.Dexterity:N0}  "
            + $"CON {database.Constitution:N0}  WIS {database.Wisdom:N0}  "
            + $"CHA {database.Charisma:N0}  INT {database.Intelligence:N0}";
        var directoryLine = $"角色目錄安全影子：HP {directory.MaxHp:N0}  MP {directory.MaxMp:N0}  "
            + $"AC {directory.ArmorClass:N0}  Lv {directory.Level:N0}  "
            + $"STR {directory.Strength:N0}  DEX {directory.Dexterity:N0}  "
            + $"CON {directory.Constitution:N0}  WIS {directory.Wisdom:N0}  "
            + $"CHA {directory.Charisma:N0}  INT {directory.Intelligence:N0}";
        if (snapshot.Runtime is { } runtime)
        {
            var runtimeLine = $"地圖實際封包值：HP {runtime.MaxHp:N0}  MP {runtime.MaxMp:N0}  "
                + $"AC {runtime.ArmorClass:N0}  Lv {runtime.Level:N0}  "
                + $"STR {runtime.Strength:N0}  DEX {runtime.Dexterity:N0}  "
                + $"CON {runtime.Constitution:N0}  WIS {runtime.Wisdom:N0}  "
                + $"CHA {runtime.Charisma:N0}  INT {runtime.Intelligence:N0}\n"
                + $"地圖延伸值：SP {runtime.SpellPowerModifier:N0}  "
                + $"MR {runtime.MagicResistanceModifier:N0}  "
                + $"火 {runtime.FireResistance:N0}  水 {runtime.WaterResistance:N0}  "
                + $"風 {runtime.WindResistance:N0}  地 {runtime.EarthResistance:N0}  "
                + $"({snapshot.Reason})";
            _extendedStats.Text = $"{heading}\n{databaseLine}\n{directoryLine}\n{runtimeLine}";
            _extendedStats.ForeColor = Color.DarkGreen;
        }
        else
        {
            _extendedStats.Text = $"{heading}\n{databaseLine}\n{directoryLine}\n"
                + "尚未進入地圖；完整值會在 S_STATUS 等擴充封包送出後顯示。";
            _extendedStats.ForeColor = snapshot.AdjustedDirectory
                ? Color.DarkOrange : Color.DarkGreen;
        }
    }

    private void StopLocalServer()
    {
        var runtime = _activeServerRuntime;
        _activeServerRuntime = null;
        runtime?.Dispose();
        _extendedStats.Text = "完整素質：Server 已停止";
        _extendedStats.ForeColor = Color.DimGray;
    }

    private async Task RunProcessAsync(ProcessStartInfo startInfo, bool trackAsLauncher = false)
    {
        using var process = new Process { StartInfo = startInfo, EnableRaisingEvents = true };
        process.OutputDataReceived += (_, args) => { if (args.Data is not null) AppendLog(args.Data); };
        process.ErrorDataReceived += (_, args) => { if (args.Data is not null) AppendLog("[ERR] " + args.Data); };
        if (!process.Start())
        {
            throw new InvalidOperationException("無法啟動 PowerShell 相容層。 ");
        }
        if (trackAsLauncher)
        {
            _activeLauncher = process;
        }
        process.BeginOutputReadLine();
        process.BeginErrorReadLine();
        await process.WaitForExitAsync();
        process.WaitForExit();
        if (process.ExitCode != 0)
        {
            throw new InvalidOperationException($"啟動流程結束碼：{process.ExitCode}");
        }
    }

    private void BrowseFolder(TextBox target)
    {
        using var dialog = new FolderBrowserDialog
        {
            InitialDirectory = Directory.Exists(target.Text) ? target.Text : Environment.CurrentDirectory,
            ShowNewFolderButton = false
        };
        if (dialog.ShowDialog(this) == DialogResult.OK)
        {
            target.Text = dialog.SelectedPath;
        }
    }

    private void OpenReports()
    {
        var directory = LauncherService.GetReportsDirectory(ReadSettingsFromUi());
        Directory.CreateDirectory(directory);
        Process.Start(new ProcessStartInfo("explorer.exe", directory) { UseShellExecute = true });
    }

    private void AppendLog(string text)
    {
        if (InvokeRequired)
        {
            BeginInvoke(() => AppendLog(text));
            return;
        }
        _log.AppendText(text + Environment.NewLine);
        _log.SelectionStart = _log.TextLength;
        _log.ScrollToCaret();
    }

    private void SetBusy(bool busy, string? status = null, bool keepLaunchDisabled = false)
    {
        _validate.Enabled = !busy;
        _buildLab.Enabled = !busy;
        _mode.Enabled = !busy;
        _displayMode.Enabled = !busy && ((_mode.SelectedItem as ModeItem)?.Mode == LaunchMode.ProtectedClient);
        _windowScale.Enabled = !busy
            && ((_mode.SelectedItem as ModeItem)?.Mode == LaunchMode.ProtectedClient)
            && ((_displayMode.SelectedItem as DisplayModeItem)?.Mode == GameDisplayMode.Windowed);
        _selectionFlow.Enabled = !busy && ((_mode.SelectedItem as ModeItem)?.Mode == LaunchMode.ProtectedClient);
        _nativeService.Enabled = !busy && ((_mode.SelectedItem as ModeItem)?.Mode == LaunchMode.ProtectedClient);
        _startLocalServer.Enabled = !busy && ((_mode.SelectedItem as ModeItem)?.Mode == LaunchMode.ProtectedClient);
        _projectRoot.Enabled = !busy;
        _clientDirectory.Enabled = !busy;
        _allowNetwork.Enabled = !busy && ((_mode.SelectedItem as ModeItem)?.Mode != LaunchMode.ProtocolLab);
        _serverProfile.Enabled = !busy && ((_mode.SelectedItem as ModeItem)?.Mode == LaunchMode.ProtectedClient);
        _loadProfile.Enabled = !busy && ((_mode.SelectedItem as ModeItem)?.Mode == LaunchMode.ProtectedClient);
        if (busy || keepLaunchDisabled)
        {
            _launch.Enabled = false;
        }
        if (status is not null)
        {
            _status.Text = status;
            _status.ForeColor = Color.DimGray;
        }
    }

    private async void OnFormClosing(object? sender, FormClosingEventArgs e)
    {
        if (_defaultMemoryMode)
        {
            if (_memoryOperation is { IsCompleted: false } || _serverControlOperation is { IsCompleted: false }
                || _serverControl?.CanClose == false)
            {
                e.Cancel = true;
                if (_closeAfterMemoryCleanup) return;
                _closeAfterMemoryCleanup = true;
                _memoryCancellation?.Cancel();
                _serverControl?.RequestStop();
                if (_memoryOperation is not null) await _memoryOperation;
                try { if (_serverControl is not null) await _serverControl.StopAsync(); }
                catch (Exception error) { AppendLog("[伺服器關閉] " + error.Message); }
                if (_serverControlOperation is not null) await _serverControlOperation;
                _closeAfterMemoryCleanup = false;
                if (!StartupOwnershipRetained && _serverControl?.CanClose != false) Close();
                else _memoryServerStatus.Text = "本次清理未確認，保留啟動器及擁有的控制。";
            }
            else if (StartupOwnershipRetained)
            {
                e.Cancel = true;
                _status.Text = "本次清理仍未完成；保留啟動器及擁有的程序控制。";
            }
            return;
        }
        if (_isolatedConfig is not null)
        {
            _isolatedTimer.Stop();
            _isolatedStartup?.Dispose();
            return;
        }
        if (_activeLauncher is { HasExited: false })
        {
            e.Cancel = true;
            MessageBox.Show(
                this,
                "遊戲或相容啟動流程仍在執行。請先正常關閉遊戲，確保暫時防火牆規則能被清理。",
                "仍在執行",
                MessageBoxButtons.OK,
                MessageBoxIcon.Information);
            return;
        }
        StopLocalServer();
    }

    private void InitializeIsolatedUi()
    {
        Text += " — 本次隔離測試準備";
        _loadingIsolatedPreferences = true;
        var preferences = LauncherMenuPreferencesStore.Load();
        _clientDirectory.Text = preferences.ClientDirectory;
        _launch.Text = "開始本次隔離測試";
        _validate.Text = "準備本次設定";
        _launch.Enabled = false;
        _extendedStats.Text = "等待 Root 協調器及新 Server；尚未取得原生啟動授權。";
        _displayMode.SelectedIndex = 0;
        _windowScale.SelectedIndex = Array.FindIndex(new[] { 100, 125, 150, 200 }, x => x == preferences.WindowScalePercent);
        _nativeService.SelectedIndex = preferences.NativeService == NativeServiceMode.RentalServer ? 1 : 0;
        _loadingIsolatedPreferences = false;
        SetIsolatedPreferencesFrozen(false);
        _status.Text = "請選擇客戶端、縮放與遊戲模式，再按「準備本次設定」。";
        foreach (var label in Controls[0].Controls.OfType<TableLayoutPanel>().SelectMany(p => p.Controls.OfType<Label>()))
            if (label.Text.StartsWith("只需選擇")) label.Text = "選擇本次設定後進行隔離準備；準備結束後可重新編輯。\n設定需納入新啟動契約並通過驗證，才能套用至遊戲。";
        _isolatedTimer.Tick += (_, _) => RefreshIsolated();
    }

    private void InitializeMemoryUi()
    {
        InitializeIsolatedUi();
        Text = "LiTo 3.81C 正式流程啟動器 0.6.71";
        _launch.Text = "啟動 LiTo.bin";
        _validate.Text = "重新檢查";
        _launch.Enabled = false;
        _extendedStats.Text = "可先調整本次遊戲設定。";
        foreach (var label in Controls[0].Controls.OfType<TableLayoutPanel>().SelectMany(p => p.Controls.OfType<Label>()))
            if (label.Text.StartsWith("選擇本次設定")) label.Text = "選擇客戶端目錄、視窗大小及遊戲模式。\n先按「啟動 LiTo.bin」，載入完成後可按「新增遊戲視窗」共用伺服器，以不同帳號測試。";
        var actions = Controls[0].Controls.OfType<FlowLayoutPanel>().Single();
        if (!_startMemoryGameOnShown) {
            actions.Controls.Add(_addClient);
            _cancelStartup.Text = "結束全部遊戲與本次伺服器";
            _multiClientTimer.Tick += (_, _) => _addClient.Enabled = !_addingClient && _developmentStartup?.CanAddClient == true && _memoryCancellation?.IsCancellationRequested == false;
            _multiClientTimer.Start();
            FormClosed += (_, _) => { _multiClientTimer.Stop(); _multiClientTimer.Dispose(); };
            _addClient.Click += async (_, _) => {
                if (_addingClient || _developmentStartup is null || _memoryCancellation is null) return;
                _addingClient = true; _addClient.Enabled = false;
                try { await _developmentStartup.AddClientAsync(_memoryCancellation.Token); }
                catch (OperationCanceledException) { }
                catch (Exception error) { AppendLog("[多開未完成] " + error.Message); }
                finally { _addingClient = false; }
            };
        }
        actions.Controls.Add(_cancelStartup);
        _cancelStartup.Click += (_, _) => _memoryCancellation?.Cancel();
        // Formal release exposes only the SQL game startup.
        var serverPanel = new FlowLayoutPanel { AutoSize = true, Dock = DockStyle.Top, FlowDirection = FlowDirection.TopDown, WrapContents = false };
        serverPanel.Controls.Add(new Label { AutoSize = true, ForeColor = Color.DimGray,
            Text = "記憶體測試伺服器可獨立啟動及停止。\n這項測試不會啟動遊戲；上方的遊戲設定仍會保留。" });
        _memoryServerStatus.Text = "尚未啟動記憶體測試伺服器。";
        serverPanel.Controls.Add(_memoryServerStatus);
        // Memory diagnostics stay in the previous diagnostic launcher.
        _memoryServerButton.Click += async (_, _) => await MemoryServerButtonAsync();
        RefreshGameAvailability();
    }

    private async Task StartMemoryServerAsync()
    {
        if (_gameStartInProgress || _memoryStartup?.OwnershipRetained == true
            || _serverControlOperation is { IsCompleted: false } || _serverControl?.CanStart == false) return;
        _serverControl ??= _serverControlFactory(AppendLog);
        _serverControl.Changed -= RefreshMemoryServerUi;
        _serverControl.Changed += RefreshMemoryServerUi;
        _serverControlOperation = RunServerControlAsync(start: true);
        await _serverControlOperation;
    }
    private async Task MemoryServerButtonAsync()
    {
        if (_gameStartInProgress) return;
        if (_serverControl?.CanStart != false) { await StartMemoryServerAsync(); return; }
        _serverControl.RequestStop();
        if (_serverControlOperation is { IsCompleted: false }) await _serverControlOperation;
        _serverControlOperation = RunServerControlAsync(start: false);
        await _serverControlOperation;
    }
    private async Task RunServerControlAsync(bool start)
    {
        try { if (start) await _serverControl!.StartAsync(); else await _serverControl!.StopAsync(); }
        catch (Exception error) { AppendLog("[記憶體測試伺服器] " + error.Message); }
        finally { RefreshMemoryServerUi(); }
    }
    private void RefreshMemoryServerUi()
    {
        if (_serverControl is null) return;
        _serverPreparing = _serverControl.State == MemoryServerControlState.Preparing;
        SetIsolatedPreferencesFrozen(_isolatedPreferencesFrozen);
        _memoryServerStatus.Text = _serverControl.Message;
        _memoryServerStatus.ForeColor = _serverControl.State == MemoryServerControlState.Failed ? Color.Firebrick : Color.DimGray;
        _memoryServerButton.Enabled = !_gameStartInProgress && _serverControl.State != MemoryServerControlState.Stopping;
        _memoryServerButton.Text = _serverControl.CanStart ? "啟動記憶體測試伺服器" :
            _serverPreparing ? "取消伺服器準備" : _serverControl.State == MemoryServerControlState.Ready ? "停止本次記憶體測試伺服器" : "重試本次伺服器清理";
        if (_defaultMemoryMode) RefreshGameAvailability();
    }

    private async Task LaunchMemoryAsync()
    {
        if (_gameStartInProgress || _memoryOperation is { IsCompleted: false } || StartupOwnershipRetained) return;
        try
        {
            if (!_startMemoryGameOnShown)
            {
                _selectedSourceId = SourceServerChoice.Load()[0].Id;
                AppendLog("[選服流程] 先開啟原生伺服器選單，選定後才輸入帳密。");
            }
            var preferences = ReadIsolatedPreferencesFromUi();
            _saveMenuPreferences(preferences);
            _gameStartInProgress = true;
            SetIsolatedPreferencesFrozen(true);
            _memoryServerButton.Enabled = false;
            _launch.Enabled = _validate.Enabled = false;
            _cancelStartup.Enabled = true;
            _memoryCancellation?.Dispose();
            _memoryCancellation = new();
            if (_startMemoryGameOnShown) _memoryStartup = new(AppendLog);
            else _developmentStartup = new(AppendLog, UpdateExtendedStats);
            _memoryOperation = RunMemoryOperationAsync(preferences, _memoryCancellation.Token);
            await _memoryOperation;
        }
        catch (Exception error) { _status.Text = "遊戲啟動未完成，請確認設定並重新檢查。"; AppendLog("[啟動] " + error.Message); }
        finally
        {
            var retained = StartupOwnershipRetained;
            _gameStartInProgress = false;
            SetIsolatedPreferencesFrozen(retained);
            _validate.Enabled = !retained;
            _cancelStartup.Enabled = retained;
            _memoryServerButton.Enabled = (!retained || (_memoryStartup?.PreChildCleanupPending == true && _serverControl?.CanStart == false))
                && _serverControl?.State != MemoryServerControlState.Stopping;
            RefreshGameAvailability();
        }
    }

    private bool CanCloseAfterMemoryGame() => _memoryOperation?.IsCompleted == true
        && _memoryStartup is { CleanupQualified: true, OwnershipRetained: false, TerminalReceiptPin: not null }
        && _memoryStartup.TerminalReceiptWriteFailure is null && _serverControl?.CanClose != false
        && (!_memoryStartup.RootControllerTerminalRequired || _memoryStartup.RootControllerTerminalPin is not null)
        && _serverControlOperation is not { IsCompleted: false };

    private async Task RunMemoryOperationAsync(LauncherMenuPreferences preferences, CancellationToken cancellation)
    {
        try
        {
            if (_startMemoryGameOnShown)
                await _memoryStartup!.RunAsync(preferences, cancellation, _startupContextRoot, PrepareOwnedTestPairForGameAsync);
            else
            {
                await PrepareOwnedTestPairForGameAsync(cancellation);
                await _developmentStartup!.RunAsync(preferences, cancellation, _selectedSourceId);
            }
            _status.Text = "本次啟動已結束並完成清理。";
        }
        catch (OperationCanceledException) { _status.Text = "本次啟動已取消並完成清理。"; }
        catch (Exception error)
        {
            var availability = _startMemoryGameOnShown ? DefaultMemoryStartupRelease.CheckAvailability(preferences) : DevelopmentStartup.CheckAvailability(preferences);
            _status.Text = availability.CanStart ? "遊戲啟動未完成，請重新檢查；詳細原因已記錄。" : availability.Message;
            _status.ForeColor = Color.Firebrick;
            AppendLog("[啟動未完成] " + error.Message);
        }
    }

    private async Task PrepareOwnedTestPairForGameAsync(CancellationToken cancellation)
    {
        var owned = _serverControl;
        if (owned is null) return;
        await DefaultMemoryStartup.PrepareOwnedTestPairAsync(async () =>
        {
            owned.RequestStop();
            if (_serverControlOperation is not null) await _serverControlOperation;
            await owned.StopAsync();
        }, () => ReferenceEquals(owned, _serverControl) && owned.CleanupQualified && !owned.OwnershipRetained
            && owned.State is not (MemoryServerControlState.Preparing or MemoryServerControlState.Ready or MemoryServerControlState.Stopping), cancellation);
        RefreshMemoryServerUi();
    }

    private MemoryStartupAvailability RefreshGameAvailability()
    {
        MemoryStartupAvailability availability;
        try { availability = _startMemoryGameOnShown ? DefaultMemoryStartupRelease.CheckAvailability(ReadIsolatedPreferencesFromUi()) : DevelopmentStartup.CheckAvailability(ReadIsolatedPreferencesFromUi()); }
        catch (ArgumentException error) { availability = new(MemoryStartupAvailabilityKind.InvalidPreferences, "請確認客戶端目錄與遊戲設定，再按「重新檢查」。", error); }
        var cleanupPending = StartupOwnershipRetained
            || (_serverControl?.State == MemoryServerControlState.Failed && _serverControl.OwnershipRetained);
        _launch.Enabled = availability.CanStart && !_gameStartInProgress && !_isolatedPreferencesFrozen && !_serverPreparing && !cleanupPending;
        if (!_gameStartInProgress)
        {
            _status.Text = cleanupPending ? "本次清理尚未確認，請先重試清理。" : availability.Message;
            _status.ForeColor = _launch.Enabled ? Color.DarkGreen : Color.DimGray;
        }
        return availability;
    }

    private LauncherMenuPreferences ReadIsolatedPreferencesFromUi() => new LauncherMenuPreferences(
        _clientDirectory.Text.Trim(),
        (_displayMode.SelectedItem as DisplayModeItem)?.Mode ?? GameDisplayMode.Windowed,
        (_windowScale.SelectedItem as ScaleItem)?.Percent ?? 100,
        (_nativeService.SelectedItem as NativeServiceItem)?.Mode ?? NativeServiceMode.StandardArena).Validate();

    private void SetIsolatedPreferencesFrozen(bool frozen)
    {
        _isolatedPreferencesFrozen = frozen;
        frozen |= _serverPreparing;
        _displayMode.Enabled = !frozen;
        _windowScale.Enabled = !frozen;
        _nativeService.Enabled = !frozen;
        _clientDirectory.ReadOnly = frozen;
        _browseClient.Enabled = !frozen;
    }

    private void IsolatedPreferencesChanged()
    {
        if (!UsesPreferenceUi || _loadingIsolatedPreferences || _isolatedPreferencesFrozen || _serverPreparing) return;
        _launch.Enabled = false;
        try { _saveMenuPreferences(ReadIsolatedPreferencesFromUi()); }
        catch (ArgumentException) { } // A partially typed path remains editable, never Start-eligible.
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        { AppendLog("設定尚未儲存：" + ex.Message); }
        if (_defaultMemoryMode) RefreshGameAvailability();
    }

    private void PublishIsolatedPreferences(string instancePath)
    {
        var preferences = _frozenIsolatedPreferences ?? throw new InvalidOperationException("尚未固定本次設定。");
        // Sibling input belongs to this new GUI instance. Issued configuration/READY and human-start schema stay immutable.
        CurrentIsolatedStartup.AtomicWriteNew(Path.Combine(instancePath, "launch-preferences.json"), new
        {
            schema = "CURRENT_GUI_LAUNCH_PREFERENCES_1", instance_path = instancePath,
            preparation_configuration_path = _isolatedConfig, preparation_configuration_sha256 = _isolatedConfigurationSha,
            preferences, requires_new_reviewed_execution_settings = true,
            native_started = false, claim_created = false, provider_lease_created = false
        });
    }

    private void PrepareIsolated()
    {
        if (_isolatedStartup is not null) { RefreshIsolated(); return; }
        try
        {
            var preferences = ReadIsolatedPreferencesFromUi();
            LauncherMenuPreferencesStore.Save(preferences);
            _isolatedConfigurationSha = null;
            if (File.Exists(_isolatedConfig))
            {
                var sha = Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(_isolatedConfig!)));
                if (_retiredIsolatedConfigurations.Contains(sha))
                    throw new InvalidOperationException("請取得新審查的準備設定；已終止的實例不可重用。");
                _isolatedConfigurationSha = sha;
            }
            _isolatedTerminal = false;
            _frozenIsolatedPreferences = preferences;
            SetIsolatedPreferencesFrozen(true);
            _validate.Enabled = false;
            _isolatedStartup = CurrentIsolatedStartup.Open(_isolatedConfig!);
            if (Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(_isolatedConfig!))) != _isolatedConfigurationSha)
                throw new InvalidOperationException("準備設定在固定選項期間改變；請取得新審查的設定。");
            PublishIsolatedPreferences(_isolatedStartup.InstancePath);
            AppendLog("[GUI] 本次實例：" + _isolatedStartup.InstancePath);
            AppendLog($"[設定] {preferences.ClientDirectory}；{preferences.WindowScalePercent}%；{preferences.NativeService}；已固定並送交新啟動契約審查。");
            _isolatedTimer.Start();
            RefreshIsolated();
        }
        catch (Exception ex) { ShowIsolatedFailure(ex.Message); }
    }

    private void RefreshIsolated()
    {
        if (_isolatedStartup is null || _isolatedTerminal) return;
        var state = _isolatedStartup.Poll();
        _launch.Enabled = state.CanStart;
        _status.Text = state.Message;
        _status.ForeColor = state.Failed ? Color.Firebrick : Color.DimGray;
        if (state.Failed) ShowIsolatedFailure(state.Message);
    }

    private void StartIsolated()
    {
        try
        {
            if (_isolatedTerminal) throw new InvalidOperationException("GUI: terminal instance; close and use a newly reviewed preparation");
            if (_isolatedStartup is null) throw new InvalidOperationException("coordinator: not prepared");
            _isolatedStartup.HumanStart();
            AppendLog("[GUI] 已記錄本次真實 Start 點擊；等待 Root 審查原生啟動契約。尚未啟動遊戲。");
            RefreshIsolated();
        }
        catch (Exception ex) { ShowIsolatedFailure(ex.Message); }
    }

    private void ShowIsolatedFailure(string reason)
    {
        _isolatedTerminal = true;
        if (_isolatedConfigurationSha is not null) _retiredIsolatedConfigurations.Add(_isolatedConfigurationSha);
        _isolatedTimer.Stop();
        var startup = _isolatedStartup;
        _isolatedStartup = null;
        startup?.LatchFailure(reason);
        try { startup?.Dispose(); }
        catch (Exception ex) { AppendLog("[準備關閉] " + ex.Message); }
        _frozenIsolatedPreferences = null;
        SetIsolatedPreferencesFrozen(false);
        _validate.Enabled = true;
        _launch.Enabled = false;
        _status.Text = "啟動準備終止 — " + reason;
        _status.ForeColor = Color.Firebrick;
        AppendLog("[終止] " + reason);
    }

    private void ReloadServerProfiles(string? preferredPath)
    {
        var normalizedPreferred = string.IsNullOrWhiteSpace(preferredPath)
            ? string.Empty
            : Path.GetFullPath(preferredPath);
        var path = ServerProfileDefinition.FindDedicatedPath();
        var profile = ServerProfileDefinition.Load(path);
        var items = new List<ProfileItem>
        {
            new(path, $"{profile.DisplayName} — {profile.Host}:{profile.Port}")
        };
        _serverProfile.Items.Clear();
        _serverProfile.Items.AddRange(items.Cast<object>().ToArray());
        var selectedIndex = items.FindIndex(item =>
            string.Equals(item.Path, normalizedPreferred, StringComparison.OrdinalIgnoreCase));
        _serverProfile.SelectedIndex = selectedIndex >= 0 ? selectedIndex : 0;
    }

    private void LoadServerProfile()
    {
        using var dialog = new OpenFileDialog
        {
            Filter = "LiTo server profile (*.json)|*.json|所有檔案 (*.*)|*.*",
            CheckFileExists = true,
            Multiselect = false,
            Title = "載入 LiTo server profile"
        };
        if (dialog.ShowDialog(this) != DialogResult.OK)
        {
            return;
        }
        try
        {
            var profile = ServerProfileDefinition.Load(dialog.FileName);
            ReloadServerProfiles(dialog.FileName);
            AppendLog($"載入角色來源：{profile.OriginSourceName} ({profile.OriginSourceId}) {profile.Host}:{profile.Port}");
        }
        catch (Exception ex)
        {
            MessageBox.Show(this, ex.Message, "無效的 Server profile", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    private sealed record ModeItem(LaunchMode Mode, string Display)
    {
        public override string ToString() => Display;
    }

    private sealed record DisplayModeItem(GameDisplayMode Mode, string Display)
    {
        public override string ToString() => Display;
    }

    private sealed record ScaleItem(int Percent, string Display)
    {
        public override string ToString() => Display;
    }

    private sealed record SelectionFlowItem(SelectionFlowMode Mode, string Display)
    {
        public override string ToString() => Display;
    }

    private sealed record NativeServiceItem(NativeServiceMode Mode, string Display)
    {
        public override string ToString() => Display;
    }

    private sealed record ProfileItem(string? Path, string Display)
    {
        public override string ToString() => Display;
    }
}
