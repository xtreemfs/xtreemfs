#pragma once


namespace XtreemFSclientutillity {

	using namespace System;
	using namespace System::ComponentModel;
	using namespace System::Collections;
	using namespace System::Windows::Forms;
	using namespace System::Data;
	using namespace System::Drawing;

	/// <summary>
	/// Zusammenfassung für Form1
	///
	/// Warnung: Wenn Sie den Namen dieser Klasse ändern, müssen Sie auch
	///          die Ressourcendateiname-Eigenschaft für das Tool zur Kompilierung verwalteter Ressourcen ändern,
	///          das allen RESX-Dateien zugewiesen ist, von denen diese Klasse abhängt.
	///          Anderenfalls können die Designer nicht korrekt mit den lokalisierten Ressourcen
	///          arbeiten, die diesem Formular zugewiesen sind.
	/// </summary>
	public ref class Form1 : public System::Windows::Forms::Form
	{
	public:
		Form1(void)
		{
			InitializeComponent();
			//
			//TODO: Konstruktorcode hier hinzufügen.
			//
		}

	protected:
		/// <summary>
		/// Verwendete Ressourcen bereinigen.
		/// </summary>
		~Form1()
		{
			if (components)
			{
				delete components;
			}
		}
	private: System::Windows::Forms::ToolStripContainer^  toolStripContainer1;
	private: System::Windows::Forms::ToolStrip^  toolStrip;
	protected: 

	private: System::Windows::Forms::ToolStripButton^  createButton;

	private: System::Windows::Forms::ToolStripButton^  mountButton;
	private: System::Windows::Forms::ToolStripButton^  unmountButton;
	private: System::Windows::Forms::ToolStripButton^  deleteButton;

	private: System::Windows::Forms::ToolStripLabel^  utilitiesLabel;
	private: System::Windows::Forms::ToolStripSeparator^  listSeparator;
	private: System::Windows::Forms::ToolStripSeparator^  mountSeparator;

	private: System::Windows::Forms::ToolStripButton^  listButton;
	private: System::Windows::Forms::Panel^  createPanel;
	private: System::Diagnostics::Process^  createProcess;
	private: System::Windows::Forms::TextBox^  mrcInputBoxC;


	private: System::Diagnostics::Process^  mountProcess;
	private: System::Diagnostics::Process^  unmountProcess;
	private: System::Windows::Forms::TextBox^  volInputBoxC;



	private: System::Windows::Forms::Label^  createMRCLabel;

	private: System::Diagnostics::Process^  deleteProcess;
	private: System::Diagnostics::Process^  listProcess;
	private: System::Windows::Forms::Label^  createVolLabel;
	private: System::Windows::Forms::ToolStripButton^  quitButton;
	private: System::Windows::Forms::Button^  proceedCreateButton;
	private: System::Windows::Forms::ToolStripSeparator^  quitSeparator;
	private: System::Windows::Forms::GroupBox^  createBox;
	private: System::Windows::Forms::GroupBox^  mountBox;
	private: System::Windows::Forms::Label^  label1;
	private: System::Windows::Forms::TextBox^  volInputBoxM;
	private: System::Windows::Forms::Label^  mountDIRLabel;
	private: System::Windows::Forms::TextBox^  dirInputBoxM;
	private: System::Windows::Forms::GroupBox^  listBox;

	private: System::Windows::Forms::Button^  mountProceedButton;
	private: System::Windows::Forms::TextBox^  mrcInputBoxL;
	private: System::Windows::Forms::Button^  listProceedButton;

	private: System::Windows::Forms::Label^  label2;
	private: System::Windows::Forms::GroupBox^  unmountBox;
	private: System::Windows::Forms::Label^  label3;

	private: System::Windows::Forms::Button^  unmountProceedButton;
	private: System::Windows::Forms::GroupBox^  deleteBox;

	private: System::Windows::Forms::Label^  label5;
	private: System::Windows::Forms::TextBox^  volInputBoxD;
	private: System::Windows::Forms::Label^  label6;
	private: System::Windows::Forms::TextBox^  mrcInputBoxD;
	private: System::Windows::Forms::Button^  deleteProceedButton;

	private: System::Windows::Forms::Label^  label4;

	private: System::Windows::Forms::ListBox^  mountPoint;
	private: System::Windows::Forms::ListBox^  umountPoint;
	private: System::Windows::Forms::Label^  lable;
	private: System::Windows::Forms::StatusStrip^  statusStrip1;
	private: System::Windows::Forms::ToolStripStatusLabel^  toolStripStatusLabel1;



	private: System::ComponentModel::IContainer^  components;

	private:
		/// <summary>
		/// Erforderliche Designervariable.
		/// </summary>


#pragma region Windows Form Designer generated code
		/// <summary>
		/// Erforderliche Methode für die Designerunterstützung.
		/// Der Inhalt der Methode darf nicht mit dem Code-Editor geändert werden.
		/// </summary>
		void InitializeComponent(void)
		{
			System::ComponentModel::ComponentResourceManager^  resources = (gcnew System::ComponentModel::ComponentResourceManager(Form1::typeid));
			this->toolStripContainer1 = (gcnew System::Windows::Forms::ToolStripContainer());
			this->createPanel = (gcnew System::Windows::Forms::Panel());
			this->statusStrip1 = (gcnew System::Windows::Forms::StatusStrip());
			this->toolStripStatusLabel1 = (gcnew System::Windows::Forms::ToolStripStatusLabel());
			this->createBox = (gcnew System::Windows::Forms::GroupBox());
			this->createMRCLabel = (gcnew System::Windows::Forms::Label());
			this->proceedCreateButton = (gcnew System::Windows::Forms::Button());
			this->createVolLabel = (gcnew System::Windows::Forms::Label());
			this->volInputBoxC = (gcnew System::Windows::Forms::TextBox());
			this->mrcInputBoxC = (gcnew System::Windows::Forms::TextBox());
			this->mountBox = (gcnew System::Windows::Forms::GroupBox());
			this->lable = (gcnew System::Windows::Forms::Label());
			this->mountPoint = (gcnew System::Windows::Forms::ListBox());
			this->mountProceedButton = (gcnew System::Windows::Forms::Button());
			this->label1 = (gcnew System::Windows::Forms::Label());
			this->volInputBoxM = (gcnew System::Windows::Forms::TextBox());
			this->mountDIRLabel = (gcnew System::Windows::Forms::Label());
			this->dirInputBoxM = (gcnew System::Windows::Forms::TextBox());
			this->listBox = (gcnew System::Windows::Forms::GroupBox());
			this->listProceedButton = (gcnew System::Windows::Forms::Button());
			this->label2 = (gcnew System::Windows::Forms::Label());
			this->mrcInputBoxL = (gcnew System::Windows::Forms::TextBox());
			this->unmountBox = (gcnew System::Windows::Forms::GroupBox());
			this->label4 = (gcnew System::Windows::Forms::Label());
			this->umountPoint = (gcnew System::Windows::Forms::ListBox());
			this->unmountProceedButton = (gcnew System::Windows::Forms::Button());
			this->deleteBox = (gcnew System::Windows::Forms::GroupBox());
			this->label5 = (gcnew System::Windows::Forms::Label());
			this->volInputBoxD = (gcnew System::Windows::Forms::TextBox());
			this->label6 = (gcnew System::Windows::Forms::Label());
			this->mrcInputBoxD = (gcnew System::Windows::Forms::TextBox());
			this->deleteProceedButton = (gcnew System::Windows::Forms::Button());
			this->toolStrip = (gcnew System::Windows::Forms::ToolStrip());
			this->utilitiesLabel = (gcnew System::Windows::Forms::ToolStripLabel());
			this->createButton = (gcnew System::Windows::Forms::ToolStripButton());
			this->mountButton = (gcnew System::Windows::Forms::ToolStripButton());
			this->mountSeparator = (gcnew System::Windows::Forms::ToolStripSeparator());
			this->listButton = (gcnew System::Windows::Forms::ToolStripButton());
			this->listSeparator = (gcnew System::Windows::Forms::ToolStripSeparator());
			this->unmountButton = (gcnew System::Windows::Forms::ToolStripButton());
			this->deleteButton = (gcnew System::Windows::Forms::ToolStripButton());
			this->quitSeparator = (gcnew System::Windows::Forms::ToolStripSeparator());
			this->quitButton = (gcnew System::Windows::Forms::ToolStripButton());
			this->label3 = (gcnew System::Windows::Forms::Label());
			this->createProcess = (gcnew System::Diagnostics::Process());
			this->mountProcess = (gcnew System::Diagnostics::Process());
			this->unmountProcess = (gcnew System::Diagnostics::Process());
			this->deleteProcess = (gcnew System::Diagnostics::Process());
			this->listProcess = (gcnew System::Diagnostics::Process());
			this->toolStripContainer1->ContentPanel->SuspendLayout();
			this->toolStripContainer1->TopToolStripPanel->SuspendLayout();
			this->toolStripContainer1->SuspendLayout();
			this->createPanel->SuspendLayout();
			this->statusStrip1->SuspendLayout();
			this->createBox->SuspendLayout();
			this->mountBox->SuspendLayout();
			this->listBox->SuspendLayout();
			this->unmountBox->SuspendLayout();
			this->deleteBox->SuspendLayout();
			this->toolStrip->SuspendLayout();
			this->SuspendLayout();
			// 
			// toolStripContainer1
			// 
			this->toolStripContainer1->BottomToolStripPanelVisible = false;
			// 
			// toolStripContainer1.ContentPanel
			// 
			this->toolStripContainer1->ContentPanel->Controls->Add(this->createPanel);
			this->toolStripContainer1->ContentPanel->Size = System::Drawing::Size(445, 145);
			this->toolStripContainer1->Dock = System::Windows::Forms::DockStyle::Fill;
			this->toolStripContainer1->LeftToolStripPanelVisible = false;
			this->toolStripContainer1->Location = System::Drawing::Point(0, 0);
			this->toolStripContainer1->Name = L"toolStripContainer1";
			this->toolStripContainer1->RightToolStripPanelVisible = false;
			this->toolStripContainer1->Size = System::Drawing::Size(445, 170);
			this->toolStripContainer1->TabIndex = 0;
			this->toolStripContainer1->Text = L"toolStripContainer1";
			// 
			// toolStripContainer1.TopToolStripPanel
			// 
			this->toolStripContainer1->TopToolStripPanel->Controls->Add(this->toolStrip);
			// 
			// createPanel
			// 
			this->createPanel->Controls->Add(this->statusStrip1);
			this->createPanel->Controls->Add(this->createBox);
			this->createPanel->Controls->Add(this->mountBox);
			this->createPanel->Controls->Add(this->listBox);
			this->createPanel->Controls->Add(this->unmountBox);
			this->createPanel->Controls->Add(this->deleteBox);
			this->createPanel->Dock = System::Windows::Forms::DockStyle::Fill;
			this->createPanel->Location = System::Drawing::Point(0, 0);
			this->createPanel->Name = L"createPanel";
			this->createPanel->Size = System::Drawing::Size(445, 145);
			this->createPanel->TabIndex = 0;
			// 
			// statusStrip1
			// 
			this->statusStrip1->Items->AddRange(gcnew cli::array< System::Windows::Forms::ToolStripItem^  >(1) {this->toolStripStatusLabel1});
			this->statusStrip1->Location = System::Drawing::Point(0, 123);
			this->statusStrip1->Name = L"statusStrip1";
			this->statusStrip1->Size = System::Drawing::Size(445, 22);
			this->statusStrip1->TabIndex = 8;
			this->statusStrip1->Text = L"statusStrip1";
			// 
			// toolStripStatusLabel1
			// 
			this->toolStripStatusLabel1->Name = L"toolStripStatusLabel1";
			this->toolStripStatusLabel1->Size = System::Drawing::Size(37, 17);
			this->toolStripStatusLabel1->Text = L"status";
			// 
			// createBox
			// 
			this->createBox->Controls->Add(this->createMRCLabel);
			this->createBox->Controls->Add(this->proceedCreateButton);
			this->createBox->Controls->Add(this->createVolLabel);
			this->createBox->Controls->Add(this->volInputBoxC);
			this->createBox->Controls->Add(this->mrcInputBoxC);
			this->createBox->Location = System::Drawing::Point(3, 3);
			this->createBox->Name = L"createBox";
			this->createBox->Size = System::Drawing::Size(439, 114);
			this->createBox->TabIndex = 5;
			this->createBox->TabStop = false;
			this->createBox->Text = L"Create volume";
			// 
			// createMRCLabel
			// 
			this->createMRCLabel->AutoSize = true;
			this->createMRCLabel->Location = System::Drawing::Point(6, 22);
			this->createMRCLabel->Name = L"createMRCLabel";
			this->createMRCLabel->Size = System::Drawing::Size(89, 13);
			this->createMRCLabel->TabIndex = 2;
			this->createMRCLabel->Text = L"Metadata Server:";
			// 
			// proceedCreateButton
			// 
			this->proceedCreateButton->Location = System::Drawing::Point(158, 79);
			this->proceedCreateButton->Name = L"proceedCreateButton";
			this->proceedCreateButton->Size = System::Drawing::Size(86, 29);
			this->proceedCreateButton->TabIndex = 4;
			this->proceedCreateButton->Text = L"Create";
			this->proceedCreateButton->UseVisualStyleBackColor = true;
			this->proceedCreateButton->Click += gcnew System::EventHandler(this, &Form1::createProceedButton_Click);
			// 
			// createVolLabel
			// 
			this->createVolLabel->AutoSize = true;
			this->createVolLabel->Location = System::Drawing::Point(6, 56);
			this->createVolLabel->Name = L"createVolLabel";
			this->createVolLabel->Size = System::Drawing::Size(45, 13);
			this->createVolLabel->TabIndex = 3;
			this->createVolLabel->Text = L"Volume:";
			// 
			// volInputBoxC
			// 
			this->volInputBoxC->Location = System::Drawing::Point(158, 53);
			this->volInputBoxC->Name = L"volInputBoxC";
			this->volInputBoxC->Size = System::Drawing::Size(226, 20);
			this->volInputBoxC->TabIndex = 1;
			// 
			// mrcInputBoxC
			// 
			this->mrcInputBoxC->Location = System::Drawing::Point(158, 19);
			this->mrcInputBoxC->Name = L"mrcInputBoxC";
			this->mrcInputBoxC->Size = System::Drawing::Size(226, 20);
			this->mrcInputBoxC->TabIndex = 0;
			// 
			// mountBox
			// 
			this->mountBox->Controls->Add(this->lable);
			this->mountBox->Controls->Add(this->mountPoint);
			this->mountBox->Controls->Add(this->mountProceedButton);
			this->mountBox->Controls->Add(this->label1);
			this->mountBox->Controls->Add(this->volInputBoxM);
			this->mountBox->Controls->Add(this->mountDIRLabel);
			this->mountBox->Controls->Add(this->dirInputBoxM);
			this->mountBox->Location = System::Drawing::Point(3, 3);
			this->mountBox->Name = L"mountBox";
			this->mountBox->Size = System::Drawing::Size(439, 114);
			this->mountBox->TabIndex = 5;
			this->mountBox->TabStop = false;
			this->mountBox->Text = L"Mount volume";
			this->mountBox->Visible = false;
			// 
			// lable
			// 
			this->lable->AutoSize = true;
			this->lable->Location = System::Drawing::Point(6, 87);
			this->lable->Name = L"lable";
			this->lable->Size = System::Drawing::Size(61, 13);
			this->lable->TabIndex = 8;
			this->lable->Text = L"Drive letter:";
			// 
			// mountPoint
			// 
			this->mountPoint->FormattingEnabled = true;
			this->mountPoint->Items->AddRange(gcnew cli::array< System::Object^  >(23) {L"Z", L"Y", L"X", L"W", L"V", L"U", L"T", L"S", 
				L"R", L"Q", L"P", L"O", L"N", L"M", L"L", L"K", L"J", L"I", L"H", L"G", L"F", L"E", L"D"});
			this->mountPoint->Location = System::Drawing::Point(158, 87);
			this->mountPoint->Name = L"mountPoint";
			this->mountPoint->Size = System::Drawing::Size(34, 17);
			this->mountPoint->TabIndex = 7;
			// 
			// mountProceedButton
			// 
			this->mountProceedButton->Location = System::Drawing::Point(298, 79);
			this->mountProceedButton->Name = L"mountProceedButton";
			this->mountProceedButton->Size = System::Drawing::Size(86, 29);
			this->mountProceedButton->TabIndex = 6;
			this->mountProceedButton->Text = L"Mount";
			this->mountProceedButton->UseVisualStyleBackColor = true;
			this->mountProceedButton->Click += gcnew System::EventHandler(this, &Form1::mountProceedButton_Click);
			// 
			// label1
			// 
			this->label1->AutoSize = true;
			this->label1->Location = System::Drawing::Point(6, 56);
			this->label1->Name = L"label1";
			this->label1->Size = System::Drawing::Size(45, 13);
			this->label1->TabIndex = 5;
			this->label1->Text = L"Volume:";
			// 
			// volInputBoxM
			// 
			this->volInputBoxM->Location = System::Drawing::Point(158, 53);
			this->volInputBoxM->Name = L"volInputBoxM";
			this->volInputBoxM->Size = System::Drawing::Size(226, 20);
			this->volInputBoxM->TabIndex = 4;
			// 
			// mountDIRLabel
			// 
			this->mountDIRLabel->AutoSize = true;
			this->mountDIRLabel->Location = System::Drawing::Point(6, 22);
			this->mountDIRLabel->Name = L"mountDIRLabel";
			this->mountDIRLabel->Size = System::Drawing::Size(86, 13);
			this->mountDIRLabel->TabIndex = 3;
			this->mountDIRLabel->Text = L"Directory Server:";
			// 
			// dirInputBoxM
			// 
			this->dirInputBoxM->Location = System::Drawing::Point(158, 19);
			this->dirInputBoxM->Name = L"dirInputBoxM";
			this->dirInputBoxM->Size = System::Drawing::Size(226, 20);
			this->dirInputBoxM->TabIndex = 1;
			// 
			// listBox
			// 
			this->listBox->Controls->Add(this->listProceedButton);
			this->listBox->Controls->Add(this->label2);
			this->listBox->Controls->Add(this->mrcInputBoxL);
			this->listBox->Location = System::Drawing::Point(3, 3);
			this->listBox->Name = L"listBox";
			this->listBox->Size = System::Drawing::Size(439, 114);
			this->listBox->TabIndex = 6;
			this->listBox->TabStop = false;
			this->listBox->Text = L"List volumes of a MRC";
			this->listBox->Visible = false;
			// 
			// listProceedButton
			// 
			this->listProceedButton->Location = System::Drawing::Point(158, 79);
			this->listProceedButton->Name = L"listProceedButton";
			this->listProceedButton->Size = System::Drawing::Size(86, 29);
			this->listProceedButton->TabIndex = 7;
			this->listProceedButton->Text = L"List";
			this->listProceedButton->UseVisualStyleBackColor = true;
			this->listProceedButton->Click += gcnew System::EventHandler(this, &Form1::listProceedButton_Click);
			// 
			// label2
			// 
			this->label2->AutoSize = true;
			this->label2->Location = System::Drawing::Point(6, 22);
			this->label2->Name = L"label2";
			this->label2->Size = System::Drawing::Size(89, 13);
			this->label2->TabIndex = 4;
			this->label2->Text = L"Metadata Server:";
			// 
			// mrcInputBoxL
			// 
			this->mrcInputBoxL->Location = System::Drawing::Point(158, 19);
			this->mrcInputBoxL->Name = L"mrcInputBoxL";
			this->mrcInputBoxL->Size = System::Drawing::Size(226, 20);
			this->mrcInputBoxL->TabIndex = 2;
			// 
			// unmountBox
			// 
			this->unmountBox->Controls->Add(this->label4);
			this->unmountBox->Controls->Add(this->umountPoint);
			this->unmountBox->Controls->Add(this->unmountProceedButton);
			this->unmountBox->Location = System::Drawing::Point(3, 3);
			this->unmountBox->Name = L"unmountBox";
			this->unmountBox->Size = System::Drawing::Size(439, 114);
			this->unmountBox->TabIndex = 6;
			this->unmountBox->TabStop = false;
			this->unmountBox->Text = L"Unmount volume";
			this->unmountBox->Visible = false;
			// 
			// label4
			// 
			this->label4->AutoSize = true;
			this->label4->Location = System::Drawing::Point(6, 22);
			this->label4->Name = L"label4";
			this->label4->Size = System::Drawing::Size(61, 13);
			this->label4->TabIndex = 12;
			this->label4->Text = L"Drive letter:";
			// 
			// umountPoint
			// 
			this->umountPoint->FormattingEnabled = true;
			this->umountPoint->Items->AddRange(gcnew cli::array< System::Object^  >(23) {L"Z", L"Y", L"X", L"W", L"V", L"U", L"T", L"S", 
				L"R", L"Q", L"P", L"O", L"N", L"M", L"L", L"K", L"J", L"I", L"H", L"G", L"F", L"E", L"D"});
			this->umountPoint->Location = System::Drawing::Point(158, 19);
			this->umountPoint->Name = L"umountPoint";
			this->umountPoint->Size = System::Drawing::Size(34, 17);
			this->umountPoint->TabIndex = 11;
			// 
			// unmountProceedButton
			// 
			this->unmountProceedButton->Location = System::Drawing::Point(158, 79);
			this->unmountProceedButton->Name = L"unmountProceedButton";
			this->unmountProceedButton->Size = System::Drawing::Size(86, 29);
			this->unmountProceedButton->TabIndex = 8;
			this->unmountProceedButton->Text = L"Unmount";
			this->unmountProceedButton->UseVisualStyleBackColor = true;
			this->unmountProceedButton->Click += gcnew System::EventHandler(this, &Form1::unmountProceedButton_Click);
			// 
			// deleteBox
			// 
			this->deleteBox->Controls->Add(this->label5);
			this->deleteBox->Controls->Add(this->volInputBoxD);
			this->deleteBox->Controls->Add(this->label6);
			this->deleteBox->Controls->Add(this->mrcInputBoxD);
			this->deleteBox->Controls->Add(this->deleteProceedButton);
			this->deleteBox->Location = System::Drawing::Point(3, 3);
			this->deleteBox->Name = L"deleteBox";
			this->deleteBox->Size = System::Drawing::Size(439, 114);
			this->deleteBox->TabIndex = 7;
			this->deleteBox->TabStop = false;
			this->deleteBox->Text = L"Delete volume";
			this->deleteBox->Visible = false;
			// 
			// label5
			// 
			this->label5->AutoSize = true;
			this->label5->Location = System::Drawing::Point(6, 56);
			this->label5->Name = L"label5";
			this->label5->Size = System::Drawing::Size(45, 13);
			this->label5->TabIndex = 12;
			this->label5->Text = L"Volume:";
			// 
			// volInputBoxD
			// 
			this->volInputBoxD->Location = System::Drawing::Point(158, 53);
			this->volInputBoxD->Name = L"volInputBoxD";
			this->volInputBoxD->Size = System::Drawing::Size(226, 20);
			this->volInputBoxD->TabIndex = 11;
			// 
			// label6
			// 
			this->label6->AutoSize = true;
			this->label6->Location = System::Drawing::Point(6, 22);
			this->label6->Name = L"label6";
			this->label6->Size = System::Drawing::Size(89, 13);
			this->label6->TabIndex = 10;
			this->label6->Text = L"Metadata Server:";
			// 
			// mrcInputBoxD
			// 
			this->mrcInputBoxD->Location = System::Drawing::Point(158, 19);
			this->mrcInputBoxD->Name = L"mrcInputBoxD";
			this->mrcInputBoxD->Size = System::Drawing::Size(226, 20);
			this->mrcInputBoxD->TabIndex = 9;
			// 
			// deleteProceedButton
			// 
			this->deleteProceedButton->Location = System::Drawing::Point(158, 79);
			this->deleteProceedButton->Name = L"deleteProceedButton";
			this->deleteProceedButton->Size = System::Drawing::Size(86, 29);
			this->deleteProceedButton->TabIndex = 8;
			this->deleteProceedButton->Text = L"Delete";
			this->deleteProceedButton->UseVisualStyleBackColor = true;
			this->deleteProceedButton->Click += gcnew System::EventHandler(this, &Form1::deleteProceedButton_Click);
			// 
			// toolStrip
			// 
			this->toolStrip->Dock = System::Windows::Forms::DockStyle::None;
			this->toolStrip->Items->AddRange(gcnew cli::array< System::Windows::Forms::ToolStripItem^  >(10) {this->utilitiesLabel, this->createButton, 
				this->mountButton, this->mountSeparator, this->listButton, this->listSeparator, this->unmountButton, this->deleteButton, this->quitSeparator, 
				this->quitButton});
			this->toolStrip->Location = System::Drawing::Point(3, 0);
			this->toolStrip->Name = L"toolStrip";
			this->toolStrip->Size = System::Drawing::Size(281, 25);
			this->toolStrip->TabIndex = 0;
			// 
			// utilitiesLabel
			// 
			this->utilitiesLabel->Name = L"utilitiesLabel";
			this->utilitiesLabel->Size = System::Drawing::Size(45, 22);
			this->utilitiesLabel->Text = L"Utilities:";
			// 
			// createButton
			// 
			this->createButton->DisplayStyle = System::Windows::Forms::ToolStripItemDisplayStyle::Text;
			this->createButton->Enabled = false;
			this->createButton->Image = (cli::safe_cast<System::Drawing::Image^  >(resources->GetObject(L"createButton.Image")));
			this->createButton->ImageTransparentColor = System::Drawing::Color::Magenta;
			this->createButton->Name = L"createButton";
			this->createButton->Size = System::Drawing::Size(44, 22);
			this->createButton->Text = L"Create";
			this->createButton->ToolTipText = L"Create a new volume.";
			this->createButton->Click += gcnew System::EventHandler(this, &Form1::createButton_Click);
			// 
			// mountButton
			// 
			this->mountButton->DisplayStyle = System::Windows::Forms::ToolStripItemDisplayStyle::Text;
			this->mountButton->Image = (cli::safe_cast<System::Drawing::Image^  >(resources->GetObject(L"mountButton.Image")));
			this->mountButton->ImageTransparentColor = System::Drawing::Color::Magenta;
			this->mountButton->Name = L"mountButton";
			this->mountButton->Size = System::Drawing::Size(41, 22);
			this->mountButton->Text = L"Mount";
			this->mountButton->ToolTipText = L"Mount a volume.";
			this->mountButton->Click += gcnew System::EventHandler(this, &Form1::mountButton_Click);
			// 
			// mountSeparator
			// 
			this->mountSeparator->Name = L"mountSeparator";
			this->mountSeparator->Size = System::Drawing::Size(6, 25);
			// 
			// listButton
			// 
			this->listButton->DisplayStyle = System::Windows::Forms::ToolStripItemDisplayStyle::Text;
			this->listButton->Image = (cli::safe_cast<System::Drawing::Image^  >(resources->GetObject(L"listButton.Image")));
			this->listButton->ImageTransparentColor = System::Drawing::Color::Magenta;
			this->listButton->Name = L"listButton";
			this->listButton->Size = System::Drawing::Size(27, 22);
			this->listButton->Text = L"List";
			this->listButton->ToolTipText = L"List available volumes.";
			this->listButton->Visible = false;
			this->listButton->Click += gcnew System::EventHandler(this, &Form1::listButton_Click);
			// 
			// listSeparator
			// 
			this->listSeparator->Name = L"listSeparator";
			this->listSeparator->Size = System::Drawing::Size(6, 25);
			this->listSeparator->Visible = false;
			// 
			// unmountButton
			// 
			this->unmountButton->DisplayStyle = System::Windows::Forms::ToolStripItemDisplayStyle::Text;
			this->unmountButton->Image = (cli::safe_cast<System::Drawing::Image^  >(resources->GetObject(L"unmountButton.Image")));
			this->unmountButton->ImageTransparentColor = System::Drawing::Color::Magenta;
			this->unmountButton->Name = L"unmountButton";
			this->unmountButton->Size = System::Drawing::Size(54, 22);
			this->unmountButton->Text = L"Unmount";
			this->unmountButton->ToolTipText = L"Unmount a volume.";
			this->unmountButton->Click += gcnew System::EventHandler(this, &Form1::unmountButton_Click);
			// 
			// deleteButton
			// 
			this->deleteButton->DisplayStyle = System::Windows::Forms::ToolStripItemDisplayStyle::Text;
			this->deleteButton->Image = (cli::safe_cast<System::Drawing::Image^  >(resources->GetObject(L"deleteButton.Image")));
			this->deleteButton->ImageTransparentColor = System::Drawing::Color::Magenta;
			this->deleteButton->Name = L"deleteButton";
			this->deleteButton->Size = System::Drawing::Size(42, 22);
			this->deleteButton->Text = L"Delete";
			this->deleteButton->ToolTipText = L"Delete an existing volume.";
			this->deleteButton->Click += gcnew System::EventHandler(this, &Form1::deleteButton_Click);
			// 
			// quitSeparator
			// 
			this->quitSeparator->Name = L"quitSeparator";
			this->quitSeparator->Size = System::Drawing::Size(6, 25);
			// 
			// quitButton
			// 
			this->quitButton->DisplayStyle = System::Windows::Forms::ToolStripItemDisplayStyle::Text;
			this->quitButton->Image = (cli::safe_cast<System::Drawing::Image^  >(resources->GetObject(L"quitButton.Image")));
			this->quitButton->ImageTransparentColor = System::Drawing::Color::Magenta;
			this->quitButton->Name = L"quitButton";
			this->quitButton->Size = System::Drawing::Size(31, 22);
			this->quitButton->Text = L"Quit";
			this->quitButton->Click += gcnew System::EventHandler(this, &Form1::quitButton_Click);
			// 
			// label3
			// 
			this->label3->Location = System::Drawing::Point(0, 0);
			this->label3->Name = L"label3";
			this->label3->Size = System::Drawing::Size(100, 23);
			this->label3->TabIndex = 0;
			// 
			// createProcess
			// 
			this->createProcess->EnableRaisingEvents = true;
			this->createProcess->StartInfo->Domain = L"";
			this->createProcess->StartInfo->ErrorDialog = true;
			this->createProcess->StartInfo->FileName = L"xtfs_mkvol";
			this->createProcess->StartInfo->LoadUserProfile = false;
			this->createProcess->StartInfo->Password = nullptr;
			this->createProcess->StartInfo->StandardErrorEncoding = nullptr;
			this->createProcess->StartInfo->StandardOutputEncoding = nullptr;
			this->createProcess->StartInfo->UserName = L"";
			this->createProcess->StartInfo->UseShellExecute = false;
			this->createProcess->SynchronizingObject = this;
			this->createProcess->Exited += gcnew System::EventHandler(this, &Form1::createProcess_Exited);
			// 
			// mountProcess
			// 
			this->mountProcess->EnableRaisingEvents = true;
			this->mountProcess->StartInfo->Domain = L"";
			this->mountProcess->StartInfo->ErrorDialog = true;
			this->mountProcess->StartInfo->FileName = L"xtfs_mount";
			this->mountProcess->StartInfo->LoadUserProfile = false;
			this->mountProcess->StartInfo->Password = nullptr;
			this->mountProcess->StartInfo->StandardErrorEncoding = nullptr;
			this->mountProcess->StartInfo->StandardOutputEncoding = nullptr;
			this->mountProcess->StartInfo->UserName = L"";
			this->mountProcess->StartInfo->UseShellExecute = false;
			this->mountProcess->SynchronizingObject = this;
			this->mountProcess->Exited += gcnew System::EventHandler(this, &Form1::mountProcess_Exited);
			// 
			// unmountProcess
			// 
			this->unmountProcess->EnableRaisingEvents = true;
			this->unmountProcess->StartInfo->Domain = L"";
			this->unmountProcess->StartInfo->ErrorDialog = true;
			this->unmountProcess->StartInfo->FileName = L"dokanctl";
			this->unmountProcess->StartInfo->LoadUserProfile = false;
			this->unmountProcess->StartInfo->Password = nullptr;
			this->unmountProcess->StartInfo->StandardErrorEncoding = nullptr;
			this->unmountProcess->StartInfo->StandardOutputEncoding = nullptr;
			this->unmountProcess->StartInfo->UserName = L"";
			this->unmountProcess->StartInfo->UseShellExecute = false;
			this->unmountProcess->SynchronizingObject = this;
			this->unmountProcess->Exited += gcnew System::EventHandler(this, &Form1::unmountProcess_Exited);
			// 
			// deleteProcess
			// 
			this->deleteProcess->EnableRaisingEvents = true;
			this->deleteProcess->StartInfo->Domain = L"";
			this->deleteProcess->StartInfo->ErrorDialog = true;
			this->deleteProcess->StartInfo->FileName = L"xtfs_rmvol";
			this->deleteProcess->StartInfo->LoadUserProfile = false;
			this->deleteProcess->StartInfo->Password = nullptr;
			this->deleteProcess->StartInfo->StandardErrorEncoding = nullptr;
			this->deleteProcess->StartInfo->StandardOutputEncoding = nullptr;
			this->deleteProcess->StartInfo->UserName = L"";
			this->deleteProcess->SynchronizingObject = this;
			this->deleteProcess->Exited += gcnew System::EventHandler(this, &Form1::deleteProcess_Exited);
			// 
			// listProcess
			// 
			this->listProcess->EnableRaisingEvents = true;
			this->listProcess->StartInfo->Domain = L"";
			this->listProcess->StartInfo->ErrorDialog = true;
			this->listProcess->StartInfo->FileName = L"xtfs_lsvol";
			this->listProcess->StartInfo->LoadUserProfile = false;
			this->listProcess->StartInfo->Password = nullptr;
			this->listProcess->StartInfo->StandardErrorEncoding = nullptr;
			this->listProcess->StartInfo->StandardOutputEncoding = nullptr;
			this->listProcess->StartInfo->UserName = L"";
			this->listProcess->SynchronizingObject = this;
			this->listProcess->Exited += gcnew System::EventHandler(this, &Form1::listProcess_Exited);
			// 
			// Form1
			// 
			this->AutoScaleDimensions = System::Drawing::SizeF(6, 13);
			this->AutoScaleMode = System::Windows::Forms::AutoScaleMode::Font;
			this->ClientSize = System::Drawing::Size(445, 170);
			this->Controls->Add(this->toolStripContainer1);
			this->Name = L"Form1";
			this->Text = L"XtreemFS client utility";
			this->toolStripContainer1->ContentPanel->ResumeLayout(false);
			this->toolStripContainer1->TopToolStripPanel->ResumeLayout(false);
			this->toolStripContainer1->TopToolStripPanel->PerformLayout();
			this->toolStripContainer1->ResumeLayout(false);
			this->toolStripContainer1->PerformLayout();
			this->createPanel->ResumeLayout(false);
			this->createPanel->PerformLayout();
			this->statusStrip1->ResumeLayout(false);
			this->statusStrip1->PerformLayout();
			this->createBox->ResumeLayout(false);
			this->createBox->PerformLayout();
			this->mountBox->ResumeLayout(false);
			this->mountBox->PerformLayout();
			this->listBox->ResumeLayout(false);
			this->listBox->PerformLayout();
			this->unmountBox->ResumeLayout(false);
			this->unmountBox->PerformLayout();
			this->deleteBox->ResumeLayout(false);
			this->deleteBox->PerformLayout();
			this->toolStrip->ResumeLayout(false);
			this->toolStrip->PerformLayout();
			this->ResumeLayout(false);

		}
#pragma endregion
private: System::Void createProceedButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->createProcess->StartInfo->Arguments = 
				 this->mrcInputBoxC->Text+"/"+this->volInputBoxC->Text;
			 this->mrcInputBoxC->ResetText();
			 this->volInputBoxC->ResetText();
			 this->createProcess->Start();
			 this->toolStripStatusLabel1->Text = "executed: "+this->createProcess->StartInfo->FileName+" "+this->createProcess->StartInfo->Arguments;
		 }
private: System::Void mountProceedButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->mountProcess->StartInfo->Arguments = 
				 this->dirInputBoxM->Text+"/"+this->volInputBoxM->Text+" "+this->mountPoint->SelectedItem;
			 this->dirInputBoxM->ResetText();
			 this->volInputBoxM->ResetText();
			 this->mountProcess->Start();
			 this->toolStripStatusLabel1->Text = "executed: "+this->mountProcess->StartInfo->FileName+" "+this->mountProcess->StartInfo->Arguments;
		 }
private: System::Void listProceedButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->listProcess->Start();
			 this->toolStripStatusLabel1->Text = "executed: "+this->listProcess->StartInfo->FileName+" "+this->listProcess->StartInfo->Arguments;
		 }
private: System::Void unmountProceedButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->unmountProcess->StartInfo->Arguments = "/u "+this->umountPoint->SelectedItem;
			 this->toolStripStatusLabel1->Text = "executed: "+this->unmountProcess->StartInfo->FileName+" "+this->unmountProcess->StartInfo->Arguments;
			 this->unmountProcess->Start();
		 }
private: System::Void deleteProceedButton_Click(System::Object^  sender, System::EventArgs^  e) {
			  this->deleteProcess->StartInfo->Arguments = 
				  this->mrcInputBoxD->Text+"/"+this->volInputBoxD->Text;
			 this->mrcInputBoxD->ResetText();
			 this->volInputBoxD->ResetText();
			 this->deleteProcess->Start();
			 this->toolStripStatusLabel1->Text = "executed: "+this->deleteProcess->StartInfo->FileName+" "+this->deleteProcess->StartInfo->Arguments;
		 }
private: System::Void createButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->createButton->Enabled = false;
			 this->mountButton->Enabled = true;
			 this->listButton->Enabled = false;
			 this->unmountButton->Enabled = true;
			 this->deleteButton->Enabled = true;

			 this->createBox->Visible = true;
			 this->mountBox->Visible = false;
			 this->listBox->Visible = false;
			 this->unmountBox->Visible = false;
			 this->deleteBox->Visible = false;
		 }
private: System::Void mountButton_Click(System::Object^  sender, System::EventArgs^  e) {
		 	 this->createButton->Enabled = true;
			 this->mountButton->Enabled = false;
			 this->listButton->Enabled = false;
			 this->unmountButton->Enabled = true;
			 this->deleteButton->Enabled = true;

			 this->createBox->Visible = false;
			 this->mountBox->Visible = true;
			 this->listBox->Visible = false;
			 this->unmountBox->Visible = false;
			 this->deleteBox->Visible = false;
		 }
private: System::Void listButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->createButton->Enabled = true;
			 this->mountButton->Enabled = true;
			 this->listButton->Enabled = false;
			 this->unmountButton->Enabled = true;
			 this->deleteButton->Enabled = true;

			 this->createBox->Visible = false;
			 this->mountBox->Visible = false;
			 this->listBox->Visible = true;
			 this->unmountBox->Visible = false;
			 this->deleteBox->Visible = false;
		 }
private: System::Void unmountButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->createButton->Enabled = true;
			 this->mountButton->Enabled = true;
			 this->listButton->Enabled = false;
			 this->unmountButton->Enabled = false;
			 this->deleteButton->Enabled = true;

			 this->createBox->Visible = false;
			 this->mountBox->Visible = false;
			 this->listBox->Visible = false;
			 this->unmountBox->Visible = true;
			 this->deleteBox->Visible = false;
		 }
private: System::Void deleteButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->createButton->Enabled = true;
			 this->mountButton->Enabled = true;
			 this->listButton->Enabled = false;
			 this->unmountButton->Enabled = true;
			 this->deleteButton->Enabled = false;

			 this->createBox->Visible = false;
			 this->mountBox->Visible = false;
			 this->listBox->Visible = false;
			 this->unmountBox->Visible = false;
			 this->deleteBox->Visible = true;
		 }
private: System::Void quitButton_Click(System::Object^  sender, System::EventArgs^  e) {
			 this->Close();
		 }
private: System::Void createProcess_Exited(System::Object^  sender, System::EventArgs^  e) {
			
		 }
private: System::Void mountProcess_Exited(System::Object^  sender, System::EventArgs^  e) {

		 }
private: System::Void listProcess_Exited(System::Object^  sender, System::EventArgs^  e) {

		 }
private: System::Void unmountProcess_Exited(System::Object^  sender, System::EventArgs^  e) {

		 }
private: System::Void deleteProcess_Exited(System::Object^  sender, System::EventArgs^  e) {

		 }
};
}
