#!/usr/bin/env perl
use warnings;
use strict;

die "perl $0 <myformat.gtf> <junctions.novel>\n" unless @ARGV eq 2;

my %hash;
open GTF, $ARGV[0] or die $!;
while(<GTF>)
{
	chomp;
	my @tmp = split;
	my ($a, $b) = (split /-/, $tmp[2])[0,1];
	$hash{$tmp[0]}{$tmp[1]}{'pn'} = $tmp[3];
	$hash{$tmp[0]}{$tmp[1]}{'rs'} = $a;
	$hash{$tmp[0]}{$tmp[1]}{'re'} = $b;
	$hash{$tmp[0]}{$tmp[1]}{'type'} = $tmp[4];
	my @a = split /,/, $tmp[5];
	my @b = split /,/, $tmp[6];
	@{$hash{$tmp[0]}{$tmp[1]}{'start'}} = @a;
	@{$hash{$tmp[0]}{$tmp[1]}{'end'}} = @b;
}
close GTF;

open NOVEL, $ARGV[1] or die $!;
open OUT, "> $ARGV[1].classify" or die $!;
print OUT "#chr\tsplice_start\tsplice_end\tsplice_strand\tsplice_reads\tsplice_type\tgene_id\tgene_strand\tgene_type\n";
while(<NOVEL>)
{
	chomp;
	my @tmp = split;
	my $js = $tmp[1] + $tmp[5];
	my $je = $tmp[2] - $tmp[6] + 1;
	my ($chr, $read, $jpn) = @tmp[0,3,4];
	my $flag = 0;
	foreach my $t(keys %{$hash{$chr}})
	{
		if($je < $hash{$chr}{$t}{'rs'} or $js > $hash{$chr}{$t}{'re'})
		{
			next;
		}else{
			$flag = 1;
			if($js < $hash{$chr}{$t}{'rs'} and $je >= $hash{$chr}{$t}{'rs'})
			{
				if($hash{$chr}{$t}{'pn'} eq '+')
				{
					if($hash{$chr}{$t}{'type'} eq 0)
					{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\tOther\t$t\t+\tNonCoding\n";
					}else{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\t5UTR\t$t\t+\tCoding\n";
					}
				}else{
					if($hash{$chr}{$t}{'type'} eq 0)
					{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\tOther\t$t\t-\tNonCoding\n";
					}else{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\t3UTR\t$t\t-\tCoding\n";
					}
				}
				last;
			}elsif($js <= $hash{$chr}{$t}{'re'} and $je > $hash{$chr}{$t}{'re'}){
				if($hash{$chr}{$t}{'pn'} eq '+')
				{
					if($hash{$chr}{$t}{'type'} eq 0)
					{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\tOther\t$t\t+\tNonCoding\n";
					}else{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\t3UTR\t$t\t+\tCoding\n";
					}
				}else{
					if($hash{$chr}{$t}{'type'} eq 0)
					{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\tOther\t$t\t-\tNonCoding\n";
					}else{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\t5UTR\t$t\t-\tCoding\n";
					}
				}
				last;
			}else{
				my ($js_stat, $je_stat) = (0, 0);
				foreach my $i(@{$hash{$chr}{$t}{'end'}})
				{
					$js_stat = 1 if($js eq $i);
				}
				foreach my $j(@{$hash{$chr}{$t}{'start'}})
				{
					$je_stat = 1 if($je eq $j);
				}
				if($js_stat eq 1 and $je_stat eq 1)
				{
					if($hash{$chr}{$t}{'type'} eq 0)
					{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\tES\t$t\t$hash{$chr}{$t}{'pn'}\tNonCoding\n";
					}else{
						print OUT "$chr\t$js\t$je\t$jpn\t$read\tES\t$t\t$hash{$chr}{$t}{'pn'}\tCoding\n";
					}
				}elsif($js_stat eq 1){
					if($hash{$chr}{$t}{'pn'} eq '+')
					{
						if($hash{$chr}{$t}{'type'} eq 0)
						{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t3S\t$t\t+\tNonCoding\n";
						}else{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t3S\t$t\t+\tCoding\n";
						}
					}else{
						if($hash{$chr}{$t}{'type'} eq 0)
						{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t5S\t$t\t-\tNonCoding\n";
						}else{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t5S\t$t\t-\tCoding\n";
						}
					}
				}elsif($je_stat eq 1){
					if($hash{$chr}{$t}{'pn'} eq '+')
					{
						if($hash{$chr}{$t}{'type'} eq 0)
						{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t5S\t$t\t+\tNonCoding\n";
						}else{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t5S\t$t\t+\tCoding\n";
						}
					}else{
						if($hash{$chr}{$t}{'type'} eq 0)
						{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t3S\t$t\t-\tNonCoding\n";
						}else{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\t3S\t$t\t-\tCoding\n";
						}
					}
				}else{
					my $f2 = 0;
					for(my $i = 0; $i < @{$hash{$chr}{$t}{'end'}}; $i ++)
					{
						if($js >= $hash{$chr}{$t}{'start'}->[$i] and $je <= $hash{$chr}{$t}{'end'}->[$i])
						{
							$f2 = 1;
							if($hash{$chr}{$t}{'type'} eq 0)
							{
								print OUT "$chr\t$js\t$je\t$jpn\t$read\tIR\t$t\t$hash{$chr}{$t}{'pn'}\tNonCoding\n";
							}else{
								print OUT "$chr\t$js\t$je\t$jpn\t$read\tIR\t$t\t$hash{$chr}{$t}{'pn'}\tCoding\n";
							}
							last;
						}
					}
					if($f2 eq 0)
					{
						if($hash{$chr}{$t}{'type'} eq 0)
						{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\tOther\t$t\t$hash{$chr}{$t}{'pn'}\tNonCoding\n";
						}else{
							print OUT "$chr\t$js\t$je\t$jpn\t$read\tOther\t$t\t$hash{$chr}{$t}{'pn'}\tCoding\n";
						}
					}
				}
				last;
			}
		}
	}
	if($flag eq 0)
	{
		print OUT "$chr\t$js\t$je\t$jpn\t$read\tIntergenic\t.\t.\t.\n";
	}
}
close NOVEL;
