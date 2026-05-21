# -*- coding: utf-8 -*-
import os
import sys
import base64
import urllib.request
import urllib.parse
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
    return "".join(c for c in text if ord(c) < 256)

def fetch_mermaid_diagram(mermaid_code):
    try:
        import zlib
        data = mermaid_code.encode('utf-8')
        compressed = zlib.compress(data, 9)
        encoded = base64.urlsafe_b64encode(compressed).decode('ascii')
        
        # Format the URL safely with kroki
        url = f"https://kroki.io/mermaid/png/{encoded}"
        
        # Download the image
        img_path = "docs/mermaid_diagram.png"
        req = urllib.request.Request(
            url, 
            headers={'User-Agent': 'Mozilla/5.0'}
        )
        with urllib.request.urlopen(req) as response:
            with open(img_path, 'wb') as out_file:
                out_file.write(response.read())
        return img_path
    except Exception as e:
        print(f"Warning: Failed to fetch Mermaid diagram from kroki: {e}")
        return None

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
    in_mermaid_block = False
    code_content = []
    mermaid_content = []
    
    # Table column widths (for 4 columns)
    col_widths = [25, 65, 45, 35]

    for line in lines:
        raw_line = sanitize_text(line.strip())
        
        # Handle code blocks
        if raw_line.startswith("```"):
            if in_code_block or in_mermaid_block:
                if in_mermaid_block:
                    in_mermaid_block = False
                    mermaid_code = "".join(mermaid_content)
                    print("Fetching Mermaid diagram...")
                    img_path = fetch_mermaid_diagram(mermaid_code)
                    if img_path and os.path.exists(img_path):
                        # Embed image dynamically
                        pdf.ln(5)
                        pdf.image(img_path, w=150)
                        pdf.ln(5)
                    else:
                        # Fallback to printing raw mermaid text
                        pdf.set_fill_color(245, 247, 250)
                        pdf.set_text_color(50, 50, 50)
                        pdf.set_font("Courier", "", 8.5)
                        pdf.multi_cell(0, 5, mermaid_code, border=1, fill=True, align="L")
                        pdf.ln(4)
                    mermaid_content = []
                else:
                    in_code_block = False
                    # Render standard code block container
                    pdf.set_fill_color(245, 247, 250)
                    pdf.set_text_color(50, 50, 50)
                    pdf.set_font("Courier", "", 8.5)
                    code_text = "".join(code_content)
                    pdf.multi_cell(0, 5, code_text, border=1, fill=True, align="L")
                    pdf.ln(4)
                    code_content = []
            else:
                if "mermaid" in raw_line:
                    in_mermaid_block = True
                else:
                    in_code_block = True
            continue

        if in_mermaid_block:
            mermaid_content.append(line)
            continue

        if in_code_block:
            code_content.append(line)
            continue

        # Skip horizontal dividers
        if raw_line == "---":
            pdf.set_draw_color(220, 224, 230)
            pdf.line(pdf.get_x(), pdf.get_y(), 210 - 20, pdf.get_y())
            pdf.ln(6)
            continue

        # Handle Tables
        if raw_line.startswith("|") and raw_line.endswith("|"):
            # Check if this is a separator row like `| :--- |`
            if ":---" in raw_line or "---:" in raw_line:
                continue
                
            # Parse table columns
            columns = [c.strip() for c in raw_line.split("|")[1:-1]]
            
            # Detect header vs content rows (headers are capitalized/bolded)
            is_header = "State" in columns or "Description" in columns
            
            if is_header:
                pdf.set_font("Helvetica", "B", 9)
                pdf.set_fill_color(224, 242, 241) # Light teal fill for headers
                pdf.set_text_color(21, 101, 192)
            else:
                pdf.set_font("Helvetica", "", 8.5)
                pdf.set_fill_color(255, 255, 255)
                pdf.set_text_color(51, 65, 85)
                
            # Draw row cells
            start_y = pdf.get_y()
            max_height = 0
            
            # First pass: calculate max height for multi_cells to prevent overflow issues
            # For simplicity, we write table rows using multi_cell with coordinate tracking
            x_offset = pdf.get_x()
            for idx, col in enumerate(columns):
                # Format code blocks inline
                clean_col = col.replace("`", "")
                pdf.set_xy(x_offset, start_y)
                
                # Check for header vs cell properties
                border = 1
                fill = True if is_header else False
                
                pdf.multi_cell(col_widths[idx], 6, clean_col, border=border, align="C" if idx != 1 else "L", fill=fill)
                curr_y = pdf.get_y()
                if curr_y - start_y > max_height:
                    max_height = curr_y - start_y
                x_offset += col_widths[idx]
                
            pdf.set_y(start_y + max_height)
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
        elif raw_line.startswith("* ") or raw_line.startswith("- ") or (len(raw_line) > 2 and raw_line[0].isdigit() and raw_line[1] == '.'):
            # Bullet/Numbered items
            is_numbered = raw_line[0].isdigit()
            bullet_char = "" if is_numbered else (chr(149) + " ")
            bullet_text = raw_line[2:] if not is_numbered else raw_line
            
            pdf.set_font("Helvetica", "", 10)
            pdf.set_text_color(51, 65, 85) # Content color
            if not is_numbered:
                pdf.write(6, bullet_char)
            
            # Detect bold inline markdown
            parts = bullet_text.split("**")
            for i, part in enumerate(parts):
                if i % 2 == 1:
                    pdf.set_font("Helvetica", "B", 10)
                else:
                    pdf.set_font("Helvetica", "", 10)
                
                # Strip code style formatting (`word`)
                pdf.write(6, part.replace("`", ""))
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
                pdf.write(6, part.replace("`", ""))
            pdf.ln(7)
        else:
            pdf.ln(3)

    pdf.output(pdf_path)
    print(f"PDF Successfully built: {pdf_path}")
    
    # Cleanup downloaded diagram if needed
    if os.path.exists("docs/mermaid_diagram.png"):
        os.remove("docs/mermaid_diagram.png")

if __name__ == "__main__":
    build_pdf(
        md_path="docs/chat_queuing_process.md",
        pdf_path="docs/chat_queuing_process.pdf"
    )
