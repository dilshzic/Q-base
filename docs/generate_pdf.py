# -*- coding: utf-8 -*-
import os
import sys
from fpdf import FPDF

class QbasePDF(FPDF):
    def header(self):
        # Premium primary accent banner
        self.set_fill_color(33, 150, 243) # Primary Blue Accent
        self.rect(0, 0, 210, 6, "F")
        self.set_y(10)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 10, "QBASE TECHNICAL ARCHITECTURE & FEATURE INTEGRATION", align="R")
        self.ln(12)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(150, 150, 150)
        self.cell(0, 10, f"Confidential & Proprietary - Page {self.page_no()}", align="C")

def sanitize_text(text):
    # Completely filter out characters outside of latin-1 encoding range to avoid FPDF errors
    return "".join(c for c in text if ord(c) < 256)

def build_pdf(md_path, pdf_path):
    pdf = QbasePDF(orientation="P", unit="mm", format="A4")
    pdf.set_margins(20, 20, 20)
    pdf.add_page()
    pdf.set_auto_page_break(auto=True, margin=20)
    
    if not os.path.exists(md_path):
        print(f"Error: {md_path} not found.")
        sys.exit(1)
        
    with open(md_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    in_code_block = False
    code_content = []

    for line in lines:
        raw_line = sanitize_text(line.strip())
        
        # Handle code blocks
        if raw_line.startswith("```"):
            if in_code_block:
                in_code_block = False
                # Render code block container
                pdf.set_fill_color(245, 247, 250)
                pdf.set_text_color(50, 50, 50)
                pdf.set_font("Courier", "", 8.5)
                
                # Render boxed content
                code_text = "".join(code_content)
                pdf.multi_cell(0, 5, code_text, border=1, fill=True, align="L")
                pdf.ln(4)
                code_content = []
            else:
                in_code_block = True
            continue

        if in_code_block:
            code_content.append(sanitize_text(line))
            continue

        # Skip horizontal dividers
        if raw_line == "---":
            pdf.set_draw_color(220, 224, 230)
            pdf.line(pdf.get_x(), pdf.get_y(), 210 - 20, pdf.get_y())
            pdf.ln(6)
            continue

        # Handle Headers
        if raw_line.startswith("# "):
            title = raw_line[2:]
            pdf.set_font("Helvetica", "B", 20)
            pdf.set_text_color(21, 101, 192) # Dark primary blue
            pdf.multi_cell(0, 10, title)
            pdf.ln(4)
        elif raw_line.startswith("## "):
            subtitle = raw_line[3:]
            pdf.set_font("Helvetica", "B", 14)
            pdf.set_text_color(30, 41, 59) # Slate Dark
            pdf.multi_cell(0, 8, subtitle)
            pdf.ln(3)
        elif raw_line.startswith("### "):
            subsubtitle = raw_line[4:]
            pdf.set_font("Helvetica", "B", 11)
            pdf.set_text_color(71, 85, 105) # Slate Medium
            pdf.multi_cell(0, 6, subsubtitle)
            pdf.ln(2)
        elif raw_line.startswith("* ") or raw_line.startswith("- "):
            # Bullet items
            bullet_text = raw_line[2:]
            pdf.set_font("Helvetica", "", 10)
            pdf.set_text_color(51, 65, 85) # Content color
            pdf.write(6, chr(149) + " ") # Elegant bullet char
            
            # Detect bold inline markdown
            parts = bullet_text.split("**")
            for i, part in enumerate(parts):
                if i % 2 == 1:
                    pdf.set_font("Helvetica", "B", 10)
                else:
                    pdf.set_font("Helvetica", "", 10)
                pdf.write(6, part)
            pdf.ln(6)
        elif raw_line != "":
            # Regular paragraphs
            pdf.set_font("Helvetica", "", 10)
            pdf.set_text_color(51, 65, 85)
            
            # Detect bold inline markdown
            parts = raw_line.split("**")
            for i, part in enumerate(parts):
                if i % 2 == 1:
                    pdf.set_font("Helvetica", "B", 10)
                else:
                    pdf.set_font("Helvetica", "", 10)
                pdf.write(6, part)
            pdf.ln(7)
        else:
            pdf.ln(3)

    pdf.output(pdf_path)
    print(f"PDF Successfully built: {pdf_path}")

if __name__ == "__main__":
    build_pdf(
        md_path="/home/dilshan/AndroidStudioProjects/Qbase/docs/PROJECT_STRUCTURE_AND_INTEGRATION.md",
        pdf_path="/home/dilshan/AndroidStudioProjects/Qbase/docs/PROJECT_STRUCTURE_AND_INTEGRATION.pdf"
    )
