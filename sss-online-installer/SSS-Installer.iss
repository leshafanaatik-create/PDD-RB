[Setup]
AppId={{7A9574D3-CCEF-4FF7-9BC1-3C08D9805360}
AppName=SSS Installer
AppVersion=5.36.1
AppPublisher=GMK
DefaultDirName={tmp}\SSS-Installer
CreateAppDir=no
Uninstallable=no
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
DisableWelcomePage=yes
DisableReadyPage=yes
DisableFinishedPage=yes
DisableProgramGroupPage=yes
CloseApplications=no
RestartApplications=no
OutputDir=out
OutputBaseFilename=SSS-Installer
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Code]
const
  PackageUrl = 'https://github.com/leshafanaatik-create/PDD-RB/releases/download/sss-v5.36.0/SSS-Setup.exe';
  PackageSha256 = 'a9448f0077ada1f377a0861de44780adc38e46160069735de85389c0cd943bc5';

function DownloadProgress(const Url, FileName: String; const Progress, ProgressMax: Int64): Boolean;
var
  Percent: Integer;
begin
  if ProgressMax > 0 then
  begin
    Percent := (Progress * 100) div ProgressMax;
    WizardForm.PreparingLabel.Caption := Format('Загрузка SSS 5.36.0... %d%%', [Percent]);
    WizardForm.Repaint;
  end
  else
    begin
    WizardForm.PreparingLabel.Caption := 'Загрузка SSS 5.36.0...';
    WizardForm.Repaint;
  end;
  Result := True;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  PackagePath: String;
  ResultCode: Integer;
begin
  Result := '';
  try
    WizardForm.PreparingLabel.Caption := 'Загрузка установочного пакета...';
    WizardForm.Repaint;
    DownloadTemporaryFile(
      PackageUrl,
      'SSS-Setup.exe',
      PackageSha256,
      @DownloadProgress);
    PackagePath := ExpandConstant('{tmp}\SSS-Setup.exe');

    WizardForm.PreparingLabel.Caption := 'Запуск установки SSS...';
    WizardForm.Repaint;

    if not Exec(PackagePath, '', '', SW_SHOW, ewWaitUntilTerminated, ResultCode) then
    begin
      Result := 'Не удалось запустить основной установщик SSS.';
      Exit;
    end;

    if ResultCode <> 0 then
    begin
      Result := Format('Основной установщик завершился с кодом %d.', [ResultCode]);
      Exit;
    end;
  except
    Result := 'Не удалось скачать или проверить установочный пакет: ' + GetExceptionMessage;
  end;
end;
